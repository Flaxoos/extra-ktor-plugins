package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database

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
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2ToDateTime
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
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
    override val application: Application,
    val client: MongoClient,
    databaseName: String
) : DatabaseTaskLockManager<MongoDbTaskLockKey>() {

    private val collection = client.getDatabase(databaseName)
        .getCollection<MongoDbTaskLockKey>("TASK_LOCKS")
        .withCodecRegistry(codecRegistry)

    override suspend fun updateTaskLockKey(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime
    ): MongoDbTaskLockKey? {
        val query = Filters.and(
            Filters.and(
                Filters.eq(MongoDbTaskLockKey::name.name, task.name),
                Filters.eq(MongoDbTaskLockKey::concurrencyIndex.name, concurrencyIndex)
            ),
            Filters.ne(MongoDbTaskLockKey::lockedAt.name, executionTime)
        )
        val updates = Updates.combine(
            Updates.set(MongoDbTaskLockKey::lockedAt.name, executionTime)
        )
        val options = FindOneAndUpdateOptions().upsert(false)
        client.startSession().use { session ->
            session.startTransaction(transactionOptions = majorityJTransaction())
            return runCatching {
                collection.findOneAndUpdate(query, updates, options).also {
                    session.commitTransaction()
                }
            }.onFailure {
                session.abortTransaction()
            }.getOrNull()
        }
    }

    override suspend fun initTaskLockKeyTable() {
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.text(MongoDbTaskLockKey::name.name),
                Indexes.ascending(MongoDbTaskLockKey::concurrencyIndex.name)
            ),
            IndexOptions().unique(true)
        )
    }

    override suspend fun insertTaskLockKey(
        task: Task,
        taskConcurrencyIndex: Int
    ): Boolean {
        client.startSession().use { session ->
            session.startTransaction(transactionOptions = majorityJTransaction())

            return collection.find(
                Filters.and(
                    Filters.eq(MongoDbTaskLockKey::name.name, task.name),
                    Filters.eq(MongoDbTaskLockKey::concurrencyIndex.name, taskConcurrencyIndex)
                )
            ).firstOrNull()?.let { false } ?: runCatching {
                collection.insertOne(
                    MongoDbTaskLockKey(
                        task.name,
                        taskConcurrencyIndex,
                        DateTime.EPOCH
                    ),
                    options = InsertOneOptions().apply {
                        comment("Initial task insertion")
                    }
                )
                true
            }.onFailure {
                session.abortTransaction()
                if (it !is MongoWriteException) throw it
            }.onSuccess {
                session.commitTransaction()
            }.getOrElse { false }
        }
    }

    override suspend fun releaseLock(key: MongoDbTaskLockKey) {}

    override fun close() {
        client.close()
    }

    private fun majorityJTransaction(): TransactionOptions = TransactionOptions.builder()
        .readConcern(ReadConcern.MAJORITY)
        .readConcern(ReadConcern.LINEARIZABLE)
        .writeConcern(WriteConcern.MAJORITY)
        .writeConcern(WriteConcern.JOURNALED)
        .build()

}

public data class MongoDbTaskLockKey(
    override val name: String,
    override val concurrencyIndex: Int,
    override var lockedAt: DateTime
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
        return reader.readString()?.format2ToDateTime()
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
