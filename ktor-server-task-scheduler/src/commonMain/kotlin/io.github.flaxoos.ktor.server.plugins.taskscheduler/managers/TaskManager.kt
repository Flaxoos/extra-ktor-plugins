package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.jvm.JvmInline

private val logger = KotlinLogging.logger { }

public abstract class TaskManager<TASK_EXECUTION_TOKEN> {
    internal abstract val name: TaskManagerConfiguration.TaskManagerName
    public abstract val application: Application
    public suspend fun execute(task: Task, executionTime: DateTime) {
        val runs = task.concurrencyRange().map { concurrencyIndex ->
            application.async {
                logger.trace { "${application.host()}: Attempting task execution at ${executionTime.format2()} for ${task.name} - $concurrencyIndex" }
                attemptExecute(task, executionTime, concurrencyIndex)?.let { key ->
                    logger.trace {
                        "${application.host()}: Starting task execution at ${executionTime.format2()} using key $key"
                    }
                    task.task.invoke(application, executionTime)
                    logger.trace { "${application.host()}: Finished task execution using key $key" }
                    key
                } ?: run {
                    logger.debug { "${application.host()}: Denied task execution for${task.name} - $concurrencyIndex at ${executionTime.format2()}" }
                    null
                }
            }
        }
        runs.awaitAll().filterNotNull().forEach {
            markExecuted(it)
        }
    }

    public abstract suspend fun init(tasks: List<Task>)
    public abstract suspend fun attemptExecute(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int
    ): TASK_EXECUTION_TOKEN?

    public abstract suspend fun markExecuted(key: TASK_EXECUTION_TOKEN)
}


@TaskSchedulerDsl
public abstract class TaskManagerConfiguration {
    public var name: String? = null
    internal abstract fun createTaskManager(application: Application): TaskManager<*>

    @JvmInline
    public value class TaskManagerName(public val value: String) {
        public companion object {
            internal const val DEFAULT_TASK_MANAGER_NAME: String = "KTOR_DEFAULT_TASK_MANAGER"
            public fun String?.toTaskManagerName(): TaskManagerName {
                require(this != DEFAULT_TASK_MANAGER_NAME) {
                    "$DEFAULT_TASK_MANAGER_NAME is a reserved name, please use another name"
                }
                return TaskManagerName(this?: DEFAULT_TASK_MANAGER_NAME)
            }
        }
    }
}