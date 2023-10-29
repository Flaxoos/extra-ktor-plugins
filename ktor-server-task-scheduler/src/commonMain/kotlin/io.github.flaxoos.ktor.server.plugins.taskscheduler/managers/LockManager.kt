package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers


import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.ktor.server.application.Application
import korlibs.time.DateTime

public interface LockManager<LOCK_KEY> {
    public val application: Application
    public suspend fun acquireLock(task: Task, executionTime: DateTime): LOCK_KEY?
    public suspend fun releaseLock(key: LOCK_KEY)
    public suspend fun init(tasks: List<Task>)
}

