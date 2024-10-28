package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.TaskLock
import korlibs.time.DateTime

public abstract class TaskLockManager<TASK_LOCK : TaskLock> : TaskManager<TASK_LOCK>() {
    public override suspend fun attemptExecute(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int,
    ): TASK_LOCK? = acquireLockKey(task, executionTime, concurrencyIndex)

    override suspend fun markExecuted(key: TASK_LOCK) {
        releaseLockKey(key)
    }

    /**
     * Get permission to execute the task
     */
    public abstract suspend fun acquireLockKey(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int,
    ): TASK_LOCK?

    /**
     * Release permission to execute the task
     */
    protected abstract suspend fun releaseLockKey(key: TASK_LOCK)
}

@TaskSchedulingDsl
public abstract class TaskLockManagerConfiguration<TASK_LOCK> : TaskManagerConfiguration<TASK_LOCK>()
