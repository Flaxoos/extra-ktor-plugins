package io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators

import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.LockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskExecutionToken
import io.ktor.server.application.Application
import io.ktor.server.application.log
import korlibs.time.DateTime

public interface TaskCoordinator {
    public val application: Application
    public suspend fun execute(task: Task, time: DateTime) {
        attemptExecute(task, time)?.let { token ->
            application.log.debug("${this::class.simpleName}: Executing task using token $token")
            task.task.invoke(application)
            markExecuted(token)
            application.log.debug("${this::class.simpleName}: Finished task execution using token $token")
        } ?: application.log.debug("${this::class.simpleName}: Denied task execution")
    }

    public suspend fun attemptExecute(task: Task, time: DateTime): TaskExecutionToken?
    public suspend fun markExecuted(token: TaskExecutionToken)
}

public class TaskLockCoordinator<LOCK_KEY>(
    public val lockManager: LockManager<LOCK_KEY>,
    override val application: Application,
    public val serialize: TaskExecutionToken.() -> LOCK_KEY,
) : TaskCoordinator {

    public override suspend fun attemptExecute(task: Task, time: DateTime): TaskExecutionToken? =
        task.executionToken(time).let { token ->
            if (lockManager.acquireLock(token.serialize())) token else null
        }

    override suspend fun markExecuted(token: TaskExecutionToken) {
        lockManager.releaseLock(token.serialize())
    }
}