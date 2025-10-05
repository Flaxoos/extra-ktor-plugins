package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.TaskLock
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.time.DateTime
import kotlin.jvm.JvmInline

private val logger = KotlinLogging.logger {}

/**
 * An abstract implementation of [TaskLockManager] using a key-value store as the lock store
 */
public abstract class KeyValueTaskLockManager(
    protected val lockExpirationMs: Long,
) : TaskLockManager<KeyValueTaskLock>() {
    /**
     * The underlying key-value store implementation
     */
    protected abstract val store: KeyValueStore

    final override suspend fun init(tasks: List<Task>) {
        logger.debug { "Initializing ${this::class.simpleName} for ${tasks.size} tasks" }
    }

    final override suspend fun acquireLockKey(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int,
    ): KeyValueTaskLock? {
        val lockKey = KeyValueTaskLock.create(task, concurrencyIndex, executionTime)
        logger.debug {
            "${application.host()}: ${executionTime.format2()}: Acquiring lock for ${task.name} - $concurrencyIndex"
        }

        val acquired =
            store.setIfNotExist(
                key = lockKey.value,
                value = executionTime.format2(),
                ttlMs = lockExpirationMs,
            )

        return if (acquired) {
            logger.debug {
                "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name} - $concurrencyIndex"
            }
            lockKey
        } else {
            logger.debug {
                "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} - $concurrencyIndex"
            }
            null
        }
    }
}

/**
 * Common implementation for key-value based task locks.
 * Uses a value class for efficient memory representation.
 *
 * The lock key format is: `taskName-***-concurrencyIndex-***-executionTime`
 */
@JvmInline
public value class KeyValueTaskLock private constructor(
    /**
     * Unique lock key: `taskName-***-concurrencyIndex-***-executionTime`
     */
    public val value: String,
) : TaskLock {
    public companion object {
        private const val DELIMITER = "-***-"

        public fun create(
            task: Task,
            concurrencyIndex: Int,
            executionTime: DateTime,
        ): KeyValueTaskLock =
            KeyValueTaskLock(
                "${task.name.replace(DELIMITER, "_")}$DELIMITER$concurrencyIndex$DELIMITER${executionTime.format2()}",
            )
    }

    override val name: String
        get() = value.split(DELIMITER, limit = 3)[0]

    override val concurrencyIndex: Int
        get() = value.split(DELIMITER, limit = 3)[1].toInt()
}

@TaskSchedulingDsl
public abstract class KeyValueTaskLockManagerConfiguration : TaskLockManagerConfiguration()
