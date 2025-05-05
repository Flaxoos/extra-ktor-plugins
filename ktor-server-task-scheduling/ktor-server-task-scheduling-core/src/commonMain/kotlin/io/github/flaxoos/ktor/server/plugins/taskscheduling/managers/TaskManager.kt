package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.utils.io.core.Closeable
import korlibs.time.DateFormat.Companion.FORMAT2
import korlibs.time.DateTime
import korlibs.time.TimeFormat.Companion.DEFAULT_FORMAT
import korlibs.time.parseLocal
import kotlin.jvm.JvmInline
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger { }

public abstract class TaskManager<TASK_EXECUTION_TOKEN : TaskExecutionToken> : Closeable {
    public abstract val name: TaskManagerConfiguration.TaskManagerName
    public abstract val application: Application

    internal suspend fun execute(
        task: Task,
        executionTime: DateTime,
    ) {
        task
            .concurrencyRange()
            .map { concurrencyIndex ->
                application.launch {
                    logger.trace {
                        "${application.host()}: Attempting task execution at ${executionTime.format2()} for ${task.name} - $concurrencyIndex"
                    }
                    attemptExecute(task, executionTime, concurrencyIndex)?.let { key ->
                        logger.trace {
                            "${application.host()}: Starting task execution at ${executionTime.format2()} using key $key"
                        }
                        task.task.invoke(application, executionTime)
                        logger.trace { "${application.host()}: Finished task execution using key $key" }
                        key
                    } ?: run {
                        logger.debug {
                            "${application.host()}: Denied task execution for${task.name} - $concurrencyIndex at ${executionTime.format2()}"
                        }
                        null
                    }
                }
            }.joinAll()
    }

    /**
     * Initialize the [TaskManager] with the given tasks it manages
     */
    public abstract suspend fun init(tasks: List<Task>)

    /**
     * Try executing the given task at the given execution time with the given concurrency index
     */
    public abstract suspend fun attemptExecute(
        task: Task,
        executionTime: DateTime,
        concurrencyIndex: Int,
    ): TASK_EXECUTION_TOKEN?

    public companion object {
        public fun Application.host(): String = "Host ${environment.config.property("ktor.deployment.host").getString()}"

        public fun DateTime.format2(): String = format(FORMAT2)

        public fun DateTime.formatTime(): String = time.format(DEFAULT_FORMAT)

        public fun String.format2ToDateTime(): DateTime = FORMAT2.parseLocal(this)
    }
}

/**
 * Configuration for [TaskManager]
 */
@TaskSchedulingDsl
public abstract class TaskManagerConfiguration {
    /**
     * The name of the task manager, will be used to identify the task manager when assigning tasks to it
     * if none is provided, it will be considered the default one. only one default task manager is allowed.
     */
    public var name: String? = null

    /**
     * Create the [TaskManager] that this configuration is for
     */
    public abstract fun createTaskManager(application: Application): TaskManager<out TaskExecutionToken>

    @JvmInline
    public value class TaskManagerName(
        public val value: String,
    ) {
        public companion object {
            private const val DEFAULT_TASK_MANAGER_NAME: String = "KTOR_DEFAULT_TASK_MANAGER"

            public fun String?.toTaskManagerName(): TaskManagerName {
                require(this != DEFAULT_TASK_MANAGER_NAME) {
                    "$DEFAULT_TASK_MANAGER_NAME is a reserved name, please use another name"
                }
                return TaskManagerName(this ?: DEFAULT_TASK_MANAGER_NAME)
            }
        }
    }
}

/**
 * Represents a token that can be used to grant permission to execute a task
 */
public interface TaskExecutionToken {
    public val name: String
    public val concurrencyIndex: Int
}
