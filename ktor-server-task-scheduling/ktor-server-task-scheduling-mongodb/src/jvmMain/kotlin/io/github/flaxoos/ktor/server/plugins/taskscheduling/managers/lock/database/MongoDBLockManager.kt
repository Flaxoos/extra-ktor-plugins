package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database

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
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2ToDateTime
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
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

/**
 * An implementation of [DatabaseTaskLockManager] using MongoDB as the lock store
 */
public class MongoDBLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    /**
     * A MongoDB client to use
     */
    private val client: MongoClient,
    /**
     * The name of the database
     */
    databaseName: String,
) : DatabaseTaskLockManager<MongoDbTaskLock>() {
    private val collection =
        client
            .getDatabase(databaseName)
            .getCollection<MongoDbTaskLock>("TASK_LOCKS")
            .withCodecRegistry(codecRegistry)

    override suspend fun updateTaskLock(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime,
    ): MongoDbTaskLock? {
        val query =
            Filters.and(
                Filters.and(
                    Filters.eq(MongoDbTaskLock::name.name, task.name),
                    Filters.eq(MongoDbTaskLock::concurrencyIndex.name, concurrencyIndex),
                    Filters.lt(MongoDbTaskLock::lockedAt.name, executionTime),
                ),
            )
        val updates =
            Updates.combine(
                Updates.set(MongoDbTaskLock::lockedAt.name, executionTime),
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

    override suspend fun initTaskLockTable() {
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(MongoDbTaskLock::name.name),
                Indexes.ascending(MongoDbTaskLock::concurrencyIndex.name),
                Indexes.ascending(MongoDbTaskLock::lockedAt.name),
            ),
            IndexOptions().unique(true),
        )
    }

    override suspend fun insertTaskLock(
        task: Task,
        taskConcurrencyIndex: Int,
    ): Boolean {
        client.startSession().use { session ->
            session.startTransaction(transactionOptions = majorityJTransaction())

            return collection
                .find(
                    Filters.and(
                        Filters.eq(MongoDbTaskLock::name.name, task.name),
                        Filters.eq(MongoDbTaskLock::concurrencyIndex.name, taskConcurrencyIndex),
                        Filters.eq(MongoDbTaskLock::lockedAt.name, DateTime.EPOCH),
                    ),
                ).firstOrNull()
                ?.let { false } ?: runCatching {
                collection.insertOne(
                    MongoDbTaskLock(
                        task.name,
                        taskConcurrencyIndex,
                        DateTime.EPOCH,
                    ),
                    options =
                        InsertOneOptions().apply {
                            comment("Initial task insertion")
                        },
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

    override fun close() {
        client.close()
    }

    private fun majorityJTransaction(): TransactionOptions =
        TransactionOptions
            .builder()
            .readConcern(ReadConcern.MAJORITY)
            .readConcern(ReadConcern.LINEARIZABLE)
            .writeConcern(WriteConcern.MAJORITY)
            .writeConcern(WriteConcern.JOURNALED)
            .build()
}

public class MongoDbTaskLock(
    override val name: String,
    override val concurrencyIndex: Int,
    override var lockedAt: DateTime,
) : DatabaseTaskLock {
    override fun toString(): String = "MongoDbTaskLockKey(name=$name, concurrencyIndex=$concurrencyIndex, lockedAt=${lockedAt.format2()})"
}

internal class MongoDbTaskLockCodec : Codec<MongoDbTaskLock> {
    override fun encode(
        writer: BsonWriter,
        value: MongoDbTaskLock,
        encoderContext: EncoderContext,
    ) {
        writer.writeStartDocument()
        writer.writeString("name", value.name)
        writer.writeInt32("concurrencyIndex", value.concurrencyIndex)
        writer.writeString("lockedAt", value.lockedAt.format2())
        writer.writeEndDocument()
    }

    override fun decode(
        reader: BsonReader,
        decoderContext: DecoderContext,
    ): MongoDbTaskLock {
        reader.readStartDocument()
        var name: String? = null
        var concurrencyIndex: Int? = null
        var lockedAt: DateTime? = null

        while (reader.readBsonType() != org.bson.BsonType.END_OF_DOCUMENT) {
            when (reader.readName()) {
                "_id" -> reader.skipValue()
                "name" -> name = reader.readString()
                "concurrencyIndex" -> concurrencyIndex = reader.readInt32()
                "lockedAt" -> lockedAt = reader.readString().format2ToDateTime()
                else -> reader.skipValue() // Skip unknown fields
            }
        }
        reader.readEndDocument()

        if (name != null && concurrencyIndex != null && lockedAt != null) {
            return MongoDbTaskLock(name = name, concurrencyIndex = concurrencyIndex, lockedAt = lockedAt)
        } else {
            throw IllegalStateException("Missing required fields in MongoDbTaskLock document")
        }
    }

    override fun getEncoderClass(): Class<MongoDbTaskLock> = MongoDbTaskLock::class.java
}

internal class DateTimeCodec : Codec<DateTime> {
    override fun encode(
        writer: BsonWriter,
        value: DateTime?,
        encoderContext: EncoderContext,
    ) {
        writer.writeString(value?.format2())
    }

    override fun getEncoderClass(): Class<DateTime> = DateTime::class.java

    override fun decode(
        reader: BsonReader,
        decoderContext: DecoderContext,
    ): DateTime? = reader.readString()?.format2ToDateTime()
}

internal val codecRegistry =
    CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(
            DateTimeCodec(),
            MongoDbTaskLockCodec(),
        ),
        MongoClientSettings.getDefaultCodecRegistry(),
    )

@TaskSchedulingDsl
public class MongoDBJobLockManagerConfiguration : DatabaseTaskLockManagerConfiguration() {
    public var client: MongoClient by Delegates.notNull()
    public var databaseName: String by Delegates.notNull()

    override fun createTaskManager(application: Application): MongoDBLockManager =
        MongoDBLockManager(
            name = name.toTaskManagerName(),
            application = application,
            client = client,
            databaseName = databaseName,
        )
}

/**
 * Add a [MongoDBLockManager]
 */
@TaskSchedulingDsl
public fun TaskSchedulingConfiguration.mongoDb(
    /**
     * The name of the task manager, will be used to identify the task manager when assigning tasks to it
     * if none is provided, it will be considered the default one. only one default task manager is allowed.
     */
    name: String? = null,
    config: MongoDBJobLockManagerConfiguration.() -> Unit,
) {
    this.addTaskManager(
        MongoDBJobLockManagerConfiguration().apply {
            config()
            this.name = name
        },
    )
}
