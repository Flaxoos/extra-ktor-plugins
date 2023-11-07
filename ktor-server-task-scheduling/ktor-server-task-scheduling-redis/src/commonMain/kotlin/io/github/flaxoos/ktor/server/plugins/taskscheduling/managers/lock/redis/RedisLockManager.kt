package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis.RedisTaskLock.Companion.toRedisLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.TaskLock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.runBlocking
import kotlin.jvm.JvmInline

internal val logger = KotlinLogging.logger { }

/**
 * An implementation of [TaskLockManager] using Redis as the lock store
 */
public class RedisLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    private val connectionPool: RedisConnectionPool,
    private val lockExpirationMs: Long,
    private val connectionAcquisitionTimeoutMs: Long
) : TaskLockManager<RedisTaskLock>() {

    override suspend fun init(tasks: List<Task>) {}

    override suspend fun acquireLockKey(task: Task, executionTime: DateTime, concurrencyIndex: Int): RedisTaskLock? =
        connectionPool.withConnection(connectionAcquisitionTimeoutMs) { redisConnection ->
            logger.debug { "${application.host()}: ${executionTime.format2()}: Acquiring lock for ${task.name} - $concurrencyIndex" }
            val key = task.toRedisLockKey(executionTime, concurrencyIndex)
            if (redisConnection.setNx(key.value, "1", lockExpirationMs) != null) {
                logger.debug { "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name} - $concurrencyIndex" }
                return@withConnection key
            }
            null
        } ?: run {
            logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} - $concurrencyIndex" }
            null
        }

    override suspend fun releaseLockKey(key: RedisTaskLock) {}

    override fun close() {
        runBlocking {
            connectionPool.closeAll()
        }
    }
}

/**
 * A [TaskLock] implementation for redis to use as a key
 */
@JvmInline
public value class RedisTaskLock internal constructor(
    /**
     *  must be unique to a task execution, i.e `$name_$concurrencyIndex at $executionTime`
     */
    public val value: String
) : TaskLock {
    public companion object {
        private const val DELIMITER = "-"
        public fun Task.toRedisLockKey(executionTime: DateTime, concurrencyIndex: Int): RedisTaskLock =
            RedisTaskLock("${name.replace(DELIMITER, "_")}-$concurrencyIndex at ${executionTime.format2()}")
    }

    override val name: String
        get() = value.split(DELIMITER, limit = 2)[0]
    override val concurrencyIndex: Int
        get() = value.split(DELIMITER, limit = 2)[1].toInt()
}

@TaskSchedulingDsl
public class RedisTaskLockManagerConfiguration(
    /**
     * The redis host
     */
    public var host: String = "undefined",

    /**
     * The redis port
     */
    public var port: Int = 0,

    /**
     * The redis username
     */
    public var username: String? = null,

    /**
     * The redis password
     */
    public var password: String? = null,

    /**
     * For how long the lock should be valid, effectively, the pxMilliseconds for the setNx command
     */
    public var lockExpirationMs: Long = 100,

    /**
     * How many connections should the pool have initially
     */
    public var connectionPoolInitialSize: Int = 10,

    /**
     * The maximum number of connections in the pool
     */
    public var connectionPoolMaxSize: Int = connectionPoolInitialSize,
    /**
     * The timeout for trying to get a connection to from the pool
     */
    public var connectionAcquisitionTimeoutMs: Long = 100
) : TaskLockManagerConfiguration<RedisTaskLock>() {
    override fun createTaskManager(application: Application): RedisLockManager =
        RedisLockManager(
            name = name.toTaskManagerName(),
            application = application,
            connectionPool = RedisConnectionPool(
                initialConnectionCount = connectionPoolInitialSize,
                host = host,
                port = port,
                username = username,
                password = password
            ),
            lockExpirationMs = lockExpirationMs,
            connectionAcquisitionTimeoutMs = connectionAcquisitionTimeoutMs
        )
}

/**
 * Add a [RedisLockManager]
 */
@TaskSchedulingDsl
public fun TaskSchedulingConfiguration.redis(

    /**
     * The name of the task manager, will be used to identify the task manager when assigning tasks to it
     * if none is provided, it will be considered the default one. only one default task manager is allowed.
     */
    name: String? = null,
    config: RedisTaskLockManagerConfiguration.() -> Unit
) {
    this.addTaskManager(
        RedisTaskLockManagerConfiguration().apply {
            config()
            this.name = name
        }
    )
}
