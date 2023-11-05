package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.time.DateTime

private val logger = KotlinLogging.logger {}

public abstract class DatabaseTaskLockManager<KEY : DatabaseTaskLockKey> : TaskLockManager<KEY>() {
    final override suspend fun init(tasks: List<Task>) {
        logger.debug { "Initializing ${this::class.simpleName} for ${tasks.size} tasks" }
        initTaskLockKeyTable()
        tasks.forEach { task ->
            task.concurrencyRange().forEach { taskConcurrencyIndex ->
                runCatching {
                    insertTaskLockKey(task, taskConcurrencyIndex)
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

    final override suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): KEY? {
        return runCatching {
            updateTaskLockKey(task, concurrencyIndex, executionTime).also {
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
     * Try to update the task lock entry in the database, where the key is the combination of task name and concurrency
     * index and execution time different from the given execution time, returning the updated entry or null if none was updated
     */
    public abstract suspend fun updateTaskLockKey(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime
    ): KEY?

    /**
     * Create the task lock key table in the database
     */
    public abstract suspend fun initTaskLockKeyTable()

    /**
     * Insert a new task lock key into the database
     */
    public abstract suspend fun insertTaskLockKey(
        task: Task,
        taskConcurrencyIndex: Int
    ): Boolean
}

public interface DatabaseTaskLockKey : TaskLockKey {
    public val name: String
    public val concurrencyIndex: Int
    public val lockedAt: DateTime
}

@TaskSchedulerDsl
public abstract class DatabaseTaskLockManagerConfiguration : TaskLockManagerConfiguration()
