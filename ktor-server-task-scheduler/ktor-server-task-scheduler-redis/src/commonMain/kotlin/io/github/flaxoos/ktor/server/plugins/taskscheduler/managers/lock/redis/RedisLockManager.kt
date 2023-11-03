package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis.RedisTaskLockKey.Companion.toRedisLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.utils.io.core.Closeable
import korlibs.time.DateTime
import kotlinx.coroutines.runBlocking

internal val logger = KotlinLogging.logger { }

public class RedisLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    private val connectionPool: RedisConnectionPool,
    private val lockExpirationMs: Long,
    private val connectionAcquisitionTimeoutMs: Long,
) : TaskLockManager<RedisTaskLockKey>() {

    override suspend fun init(tasks: List<Task>) {}

    override suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): RedisTaskLockKey? =
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

    override suspend fun releaseLock(key: RedisTaskLockKey) {}

    override fun close() {
        runBlocking {
            connectionPool.closeAll()
        }
    }
}

@JvmInline
public value class Retry(public val freqMs: Long)

/**
 * value must be unique to a task execution, i.e name + executionTime
 */
@JvmInline
public value class RedisTaskLockKey(public val value: String) : TaskLockKey {
    public companion object {
        public fun Task.toRedisLockKey(executionTime: DateTime, concurrencyIndex: Int): RedisTaskLockKey =
            RedisTaskLockKey("$name-$concurrencyIndex, ${executionTime.format2()}")
    }
}

@TaskSchedulerDsl
public class RedisTaskLockManagerConfiguration(
    public var host: String = "localhost",
    public var port: Int = 8080,
    public var lockExpirationMs: Long = 100,
    public var connectionPoolSize: Int = 10
) : TaskLockManagerConfiguration() {
    override fun createTaskManager(application: Application): TaskManager<*> =
        RedisLockManager(
            name = name.toTaskManagerName(),
            application = application,
            connectionPool = RedisConnectionPool(
                size = connectionPoolSize,
                host = host,
                port = port
            ),
            lockExpirationMs = lockExpirationMs,
            connectionAcquisitionTimeoutMs = connectionAcquisitionTimeoutMs,
        )
}

@TaskSchedulerDsl
public fun TaskSchedulerConfiguration.redis(
    name: String? = null,
    config: RedisTaskLockManagerConfiguration.() -> Unit
) {
    this.addTaskManager { application ->
        RedisTaskLockManagerConfiguration().apply {
            config()
            this.name = name
        }.createTaskManager(application)
    }
}
