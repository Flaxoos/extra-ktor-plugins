package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskLockCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.DatabaseTaskLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.JdbcLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.JdbcTaskLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.MongoDBLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.MongoDbTaskLockKey
import io.ktor.server.application.Application
import kotlin.properties.Delegates
import org.jetbrains.exposed.sql.Database as ExposedDatabase

@TaskSchedulerDsl
public class JdbcJobLockManagerConfig {
    public var database: ExposedDatabase by Delegates.notNull()
    public var lockExpirationMs: Long = 5000
}

@TaskSchedulerDsl
public class MongoDBJobLockManagerConfig {
    public var client: MongoClient by Delegates.notNull()
    public var databaseName: String by Delegates.notNull()
    public var lockExpirationMs: Long = 5000
}

public abstract class Database<T : DatabaseTaskLockKey>() : Lock<T>() {

    public class Jdbc(private val config: JdbcJobLockManagerConfig.() -> Unit) : Database<JdbcTaskLockKey>() {
        override fun createCoordinator(application: Application): TaskCoordinator<JdbcTaskLockKey> =
            with(JdbcJobLockManagerConfig().apply(config)) {
                TaskLockCoordinator(
                    JdbcLockManager(
                        application = application,
                        lockExpirationMs = lockExpirationMs,
                        database = database
                    ), application
                )
            }
    }

    public class MongoDB(private val config: MongoDBJobLockManagerConfig.() -> Unit) : Database<MongoDbTaskLockKey>() {
        override fun createCoordinator(application: Application): TaskCoordinator<MongoDbTaskLockKey> =
            with(MongoDBJobLockManagerConfig().apply(config)) {
                TaskLockCoordinator(
                    MongoDBLockManager(
                        application = application,
                        lockExpirationMs = lockExpirationMs,
                        client = client,
                        databaseName = databaseName
                    ), application
                )
            }
    }
}