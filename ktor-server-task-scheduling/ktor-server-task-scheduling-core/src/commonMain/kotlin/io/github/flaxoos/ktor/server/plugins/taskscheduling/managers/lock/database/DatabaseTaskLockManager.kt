package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.TaskLock
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.time.DateTime

private val logger = KotlinLogging.logger {}

/**
 * An abstract implementation of [TaskLockManager] using a database as the lock store
 */
public abstract class DatabaseTaskLockManager<DB_TASK_LOCK_KEY : DatabaseTaskLock> :
    TaskLockManager<DB_TASK_LOCK_KEY>() {
    final override suspend fun init(tasks: List<Task>) {
        logger.debug { "Initializing ${this::class.simpleName} for ${tasks.size} tasks" }
        initTaskLockTable()
        tasks.forEach { task ->
            task.concurrencyRange().forEach { taskConcurrencyIndex ->
                runCatching {
                    insertTaskLock(task, taskConcurrencyIndex)
                }.onFailure {
                    logger.error(it) { "${this::class.simpleName} failed to insert task during initialization" }
                }.getOrNull()?.let {
                    if (it) {
                        logger.debug { "${this::class.simpleName} inserted task lock key during initialization" }
                    }
                }
            }
        }
    }

    final override suspend fun acquireLockKey(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int,
    ): DB_TASK_LOCK_KEY? {
        return runCatching {
            updateTaskLock(task, concurrencyIndex, executionTime).also {
                if (it == null) {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Could not acquire lock for ${task.name} - $concurrencyIndex" }
                } else {
                    logger.debug {
                        "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name} - $concurrencyIndex"
                    }
                }
            }
        }.onFailure {
            logger.warn { "${application.host()}: ${executionTime.format2()}: Failed acquiring lock for ${task.name} - $concurrencyIndex: ${it.message}" }
        }.getOrNull()
    }

    /**
     * Create the task lock key table in the database
     */
    public abstract suspend fun initTaskLockTable()

    /**
     * Insert a new task lock key into the database
     */
    public abstract suspend fun insertTaskLock(
        task: Task,
        taskConcurrencyIndex: Int,
    ): Boolean

    /**
     * Try to update the task lock entry in the database, where the key is the combination of task name and concurrency
     * index and execution time different from the given execution time, returning the updated entry or null if none was updated
     */
    public abstract suspend fun updateTaskLock(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime,
    ): DB_TASK_LOCK_KEY?
}

public interface DatabaseTaskLock : TaskLock {
    public val lockedAt: DateTime
}

@TaskSchedulingDsl
public abstract class DatabaseTaskLockManagerConfiguration<DB_TASK_LOCK_KEY : DatabaseTaskLock> : TaskLockManagerConfiguration<DB_TASK_LOCK_KEY>()
