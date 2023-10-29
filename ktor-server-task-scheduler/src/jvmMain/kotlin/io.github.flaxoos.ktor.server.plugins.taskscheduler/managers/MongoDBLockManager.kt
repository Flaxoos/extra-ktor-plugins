package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers

import com.mongodb.DuplicateKeyException
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.MongoWriteException
import com.mongodb.TransactionOptions
import com.mongodb.WriteConcern
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertOneOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.toDateTimeFormat2
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries

private val logger = KotlinLogging.logger {}

internal class MongoDBLockManager(
    override val application: Application, private val lockExpirationMs: Long,
    val client: MongoClient,
    databaseName: String
) : DatabaseLockManager<MongoDbTaskLockKey>() {
    private val collection = client.getDatabase(databaseName)
        .getCollection<MongoDbTaskLockKey>("TASK_LOCKS")
        .withCodecRegistry(codecRegistry)

    override suspend fun init(tasks: List<Task>) {
        try {
            collection.createIndex(Indexes.text("name"), IndexOptions().unique(true))
        } catch (_: DuplicateKeyException) {
        }
        tasks.forEach { task ->
            client.startSession().use { session ->
                session.startTransaction(transactionOptions = majority_J_TX())

                collection.find(Filters.eq(MongoDbTaskLockKey::name.name, task.name)).firstOrNull()
                    ?: run {
                        try {
                            collection.insertOne(MongoDbTaskLockKey(
                                task.name,
                                null,
                                null
                            ), options = InsertOneOptions().apply {
                                comment("Initial task insertion")
                            })
                        } catch (_: MongoWriteException) {
                            session.abortTransaction()
                            return@use
                        }
                    }
                session.commitTransaction()
            }
        }
    }

    override suspend fun acquireLock(task: Task, executionTime: DateTime): MongoDbTaskLockKey? {
        val query = Filters.and(
            Filters.eq(MongoDbTaskLockKey::name.name, task.name),
            Filters.or(
                Filters.eq(MongoDbTaskLockKey::lockedAt.name, null),
                Filters.eq(MongoDbTaskLockKey::lockUntil.name, null),
                Filters.lt(MongoDbTaskLockKey::lockUntil.name, executionTime)
            )
        )
        val updates = Updates.combine(
            Updates.set(MongoDbTaskLockKey::lockedAt.name, executionTime.unixMillis),
            Updates.set(MongoDbTaskLockKey::lockUntil.name, executionTime.unixMillis.plus(lockExpirationMs)),
        )
        val options = FindOneAndUpdateOptions().upsert(false)
        client.startSession().use { session ->
            return try {
                session.startTransaction(transactionOptions = majority_J_TX())
                val updateResult = collection.findOneAndUpdate(query, updates, options)
                session.commitTransaction()
                return if (updateResult != null) {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name}" }
                    updateResult
                } else {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} as no document was updated" }
                    null
                }
            } catch (e: MongoException) {
                logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name}: ${e.message}" }
                null
            }
        }
    }

    private fun majority_J_TX(): TransactionOptions = TransactionOptions.builder()
        .writeConcern(WriteConcern.MAJORITY)
        .writeConcern(WriteConcern.JOURNALED)
        .build()

    override suspend fun releaseLock(key: MongoDbTaskLockKey) {
        val query = Filters.eq(MongoDbTaskLockKey::name.name, key.name)
        val updates = Updates.combine(
            Updates.set(MongoDbTaskLockKey::lockedAt.name, null),
            Updates.set(MongoDbTaskLockKey::lockUntil.name, null),
        )
        val options = UpdateOptions()
        collection.updateOne(query, updates, options)
    }
}

public data class MongoDbTaskLockKey(
    override val name: String,
    override var lockedAt: DateTime?,
    override var lockUntil: DateTime?
) : DatabaseTaskLockKey {
    override fun toString(): String {
        return "MongoDbTaskLockKey(name=$name, lockedAt=${lockedAt?.format2()}, lockUntil=${lockUntil?.format2()})"
    }
}

public class DateTimeCodec : Codec<DateTime> {
    override fun encode(writer: BsonWriter, value: DateTime?, encoderContext: EncoderContext) {
        writer.writeString(value?.format2())
    }

    override fun getEncoderClass(): Class<DateTime> {
        return DateTime::class.java
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): DateTime? {
        return reader.readString()?.toDateTimeFormat2()
    }

}

internal val codecRegistry = CodecRegistries.fromRegistries(
    CodecRegistries.fromCodecs(DateTimeCodec()),
    MongoClientSettings.getDefaultCodecRegistry()
)
