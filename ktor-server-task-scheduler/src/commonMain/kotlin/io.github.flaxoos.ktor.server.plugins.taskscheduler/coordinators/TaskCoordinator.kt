package io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators

import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.LockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime

private val logger = KotlinLogging.logger { }

public interface TaskCoordinator<TASK_LOCK_KEY> {
    public val application: Application
    public suspend fun execute(task: Task, executionTime: DateTime) {
        logger.trace { "${application.host()}: Attempting task execution at ${executionTime.format2()} for ${task.name}" }
        attemptExecute(task,executionTime)?.let { key ->
            logger.debug {
                "${application.host()}: Starting task execution at ${executionTime.format2()} using key $key"
            }
            task.task.invoke(application, executionTime)
            markExecuted(key)
            logger.trace { "${application.host()}: Finished task execution using key $key" }
        } ?: logger.debug { "${application.host()}: Denied task execution for ${task.name} at ${executionTime.format2()}" }
    }

    public suspend fun attemptExecute(task: Task, executionTime: DateTime): TASK_LOCK_KEY?
    public suspend fun markExecuted(key: TASK_LOCK_KEY)
}

public class TaskLockCoordinator<TASK_LOCK_KEY : TaskLockKey>(
    public val lockManager: LockManager<TASK_LOCK_KEY>,
    public override val application: Application,
) : TaskCoordinator<TASK_LOCK_KEY> {

    public suspend fun init(tasks: List<Task>) {
        lockManager.init(tasks)
    }

    public override suspend fun attemptExecute(task: Task, executionTime: DateTime): TASK_LOCK_KEY? =
        lockManager.acquireLock(task,executionTime)

    override suspend fun markExecuted(key: TASK_LOCK_KEY) {
        lockManager.releaseLock(key)
    }
}