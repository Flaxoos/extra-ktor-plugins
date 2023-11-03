package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import korlibs.time.DateTime

public abstract class TaskLockManager<TASK_LOCK_KEY : TaskLockKey> : TaskManager<TASK_LOCK_KEY>() {

    public override suspend fun attemptExecute(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int
    ): TASK_LOCK_KEY? =
        acquireLock(task, executionTime, concurrencyIndex)

    override suspend fun markExecuted(key: TASK_LOCK_KEY) {
        releaseLock(key)
    }

    public abstract suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): TASK_LOCK_KEY?
    public abstract suspend fun releaseLock(key: TASK_LOCK_KEY)
}

@TaskSchedulerDsl
public abstract class TaskLockManagerConfiguration : TaskManagerConfiguration() {
    public var connectionAcquisitionTimeoutMs: Long = 100
}
