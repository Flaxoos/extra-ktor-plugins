package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.mongodb

import com.mongodb.MongoClientSettings
import com.mongodb.MongoWriteException
import com.mongodb.ReadConcern
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
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.DatabaseTaskLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.DatabaseTaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.DatabaseTaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.toDateTimeFormat2
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}

internal class MongoDBLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application, private val lockExpirationMs: Long,
    val client: MongoClient,
    databaseName: String
) : DatabaseTaskLockManager<MongoDbTaskLockKey>() {
    private val collection = client.getDatabase(databaseName)
        .getCollection<MongoDbTaskLockKey>("TASK_LOCKS")
        .withCodecRegistry(codecRegistry)

    override suspend fun init(tasks: List<Task>) {
        runCatching {
            collection.createIndex(
                Indexes.compoundIndex(
                    Indexes.text(MongoDbTaskLockKey::name.name),
                    Indexes.ascending(MongoDbTaskLockKey::concurrencyIndex.name)
                ),
                IndexOptions().unique(true)
            )
        }
        tasks.forEach { task ->
            task.concurrencyRange().forEach { taskConcurrencyIndex ->
                client.startSession().use { session ->
                    session.startTransaction(transactionOptions = majorityJTransaction())

                    collection.find(
                        Filters.and(
                            Filters.eq(MongoDbTaskLockKey::name.name, task.name),
                            Filters.eq(MongoDbTaskLockKey::concurrencyIndex.name, taskConcurrencyIndex)
                        )
                    ).firstOrNull()
                        ?: runCatching {
                            collection.insertOne(
                                MongoDbTaskLockKey(
                                    task.name,
                                    taskConcurrencyIndex,
                                    DateTime.EPOCH,
                                ), options = InsertOneOptions().apply {
                                    comment("Initial task insertion")
                                })
                        }.onFailure {
                            session.abortTransaction()
                            if (it !is MongoWriteException) throw it
                        }.onSuccess {
                            session.commitTransaction()
                        }
                }
            }
        }
    }

    override suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): MongoDbTaskLockKey? {
        val query = Filters.and(
            Filters.and(
                Filters.eq(MongoDbTaskLockKey::name.name, task.name),
                Filters.eq(MongoDbTaskLockKey::concurrencyIndex.name, concurrencyIndex)
            ),
            Filters.ne(MongoDbTaskLockKey::lockedAt.name, executionTime)
        )
        val updates = Updates.combine(
            Updates.set(MongoDbTaskLockKey::lockedAt.name, executionTime),
        )
        val options = FindOneAndUpdateOptions().upsert(false)
        client.startSession().use { session ->
            return runCatching {
                session.startTransaction(transactionOptions = majorityJTransaction())
                val updateResult = collection.findOneAndUpdate(query, updates, options)
                session.commitTransaction()
                return if (updateResult != null) {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name}" }
                    updateResult
                } else {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} as no document was updated" }
                    null
                }
            }.onFailure { logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name}: ${it.message}" } }
                .getOrNull()
        }
    }

    private fun majorityJTransaction(): TransactionOptions = TransactionOptions.builder()
        .readConcern(ReadConcern.MAJORITY)
        .readConcern(ReadConcern.LINEARIZABLE)
        .writeConcern(WriteConcern.MAJORITY)
        .writeConcern(WriteConcern.JOURNALED)
        .build()

    override suspend fun releaseLock(key: MongoDbTaskLockKey) {}
}

public data class MongoDbTaskLockKey(
    override val name: String,
    override val concurrencyIndex: Int,
    override var lockedAt: DateTime,
) : DatabaseTaskLockKey {
    override fun toString(): String {
        return "MongoDbTaskLockKey(name=$name, concurrencyIndex=$concurrencyIndex, lockedAt=${lockedAt.format2()})"
    }
}

internal class DateTimeCodec : Codec<DateTime> {
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

@TaskSchedulerDsl
public class MongoDBJobLockManagerConfiguration : DatabaseTaskLockManagerConfiguration() {
    public var client: MongoClient by Delegates.notNull()
    public var databaseName: String by Delegates.notNull()
    override fun createTaskManager(application: Application): TaskManager<*> =
        MongoDBLockManager(
            name = name.toTaskManagerName(),
            application = application,
            lockExpirationMs = lockExpirationMs,
            client = client,
            databaseName = databaseName
        )
}

@TaskSchedulerDsl
public fun TaskSchedulerConfiguration.mongoDb(
    name: String? = null,
    config: MongoDBJobLockManagerConfiguration.() -> Unit
) {
    this.addTaskManager { application ->
        MongoDBJobLockManagerConfiguration().apply {
            config()
            this.name = name
        }.createTaskManager(application)
    }
}
