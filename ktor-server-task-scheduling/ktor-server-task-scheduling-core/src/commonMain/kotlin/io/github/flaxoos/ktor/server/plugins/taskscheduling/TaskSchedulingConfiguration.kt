package io.github.flaxoos.ktor.server.plugins.taskscheduling

import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.buildSchedule
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher

@DslMarker
public annotation class TaskSchedulingDsl

/**
 * Configuration for [TaskScheduling]
 */
@TaskSchedulingDsl
public open class TaskSchedulingConfiguration {
    internal val tasks = mutableMapOf<TaskManagerName, MutableList<Task>>()
    internal val taskManagers = mutableListOf<(Application) -> TaskManager<*>>()

    /**
     * Add a task to be managed by a [TaskManager] with the given name or the default one if no name is provided and a
     * default task manager has been configured
     */
    @TaskSchedulingDsl
    public fun task(
        /**
         * The name of the [TaskManager] to manage the task. if not provided, the default task manager will be used,
         * if configured, if not configured an error would be thrown
         */
        taskManagerName: String? = null,
        /**
         * The configuration of the task
         */
        taskConfiguration: TaskConfiguration.() -> Unit
    ) {
        tasks.getOrPut(taskManagerName.toTaskManagerName()) { mutableListOf() }.add(
            TaskConfiguration().apply(taskConfiguration).also { checkNotNull(it.kronSchedule) }.let {
                Task(
                    name = it.name,
                    dispatcher = it.dispatcher,
                    concurrency = it.concurrency,
                    kronSchedule = buildSchedule(it.kronSchedule ?: error("No kron schedule configured")),
                    task = it.task
                )
            }
        )
    }

    public fun addTaskManager(taskManagerConfiguration: TaskManagerConfiguration<*>) {
        taskManagers.add {
            taskManagerConfiguration.createTaskManager(it)
        }
    }
}

/**
 * Configuration for a [Task]
 */
@TaskSchedulingDsl
public class TaskConfiguration {
    /**
     * The name of the task, should be unique, as it id used to identify the task
     */
    public var name: String = ANONYMOUS_TASK_NAME

    /**
     * The [kronSchedule] for the task
     */
    public var kronSchedule: (SchedulerBuilder.() -> Unit)? = null

    /**
     * How many instances of the task should be fired at the same time
     */
    public var concurrency: Int = 1

    /**
     * What dispatcher should be used to execute the task, if none is provided, the application's dispatcher will be used
     */
    public var dispatcher: CoroutineDispatcher? = null

    /**
     * The actual task logic
     */
    public var task: suspend Application.(DateTime) -> Unit = {}

    private companion object {
        const val ANONYMOUS_TASK_NAME = "Anonymous_Task"
    }
}
