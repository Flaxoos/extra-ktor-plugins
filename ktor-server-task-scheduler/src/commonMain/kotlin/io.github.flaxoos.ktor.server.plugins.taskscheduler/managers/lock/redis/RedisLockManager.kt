package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.flaxoos.github.knedis.RedisConnectionPool
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.DEFAULT_TASK_MANAGER_NAME
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis.RedisTaskLockKey.Companion.toRedisLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger { }

public class RedisLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    private val connectionPool: RedisConnectionPool,
    private val lockExpirationMs: Long,
    private val lockAcquisitionTimeoutMs: Long,
    private val retry: Retry?,
) : TaskLockManager<RedisTaskLockKey>() {

    override suspend fun init(tasks: List<Task>) {}

    override suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): RedisTaskLockKey? =
        connectionPool.withConnection { redisConnection ->
            logger.debug { "${application.host()}: Acquiring lock for $task" }
            val key = task.toRedisLockKey(executionTime, concurrencyIndex)
            withTimeoutOrNull(lockAcquisitionTimeoutMs.milliseconds) {
                while (true) {
                    if (redisConnection.set(key.value, "1", lockExpirationMs) != null) {
                        logger.debug { "${application.host()}: Acquired lock for $task" }
                        return@withTimeoutOrNull key
                    }
                    retry?.let { delay(it.freqMs) } ?: break
                }
                null
            } ?: run {
                logger.debug { "${application.host()}: Unable to acquire lock for $task" }
                null
            }
        } ?: run {
            logger.debug { "${application.host()}: No connection available for acquiring lock for $task" }
            null
        }

    override suspend fun releaseLock(key: RedisTaskLockKey) {
        connectionPool.withConnection { redisConnection ->
            redisConnection.del(key.value)
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
    public var connectionPoolSize: Int = 10,
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
            lockAcquisitionTimeoutMs = lockAcquisitionTimeoutMs,
            retry = null//  redisJobLockManagerConfig.lockAcquisitionRetryFreqMs,
        )
}

@TaskSchedulerDsl
public fun TaskSchedulerConfiguration.redis(name: String? = null, config: RedisTaskLockManagerConfiguration.() -> Unit) {
    this.addTaskManager { application ->
        RedisTaskLockManagerConfiguration().apply{
            config()
            this.name = name
        }.createTaskManager(application)
    }
}
