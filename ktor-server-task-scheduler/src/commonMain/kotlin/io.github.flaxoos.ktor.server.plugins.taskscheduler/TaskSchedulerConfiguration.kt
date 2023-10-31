package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.buildSchedule
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.DEFAULT_TASK_MANAGER_NAME
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@DslMarker
public annotation class TaskSchedulerDsl

@TaskSchedulerDsl
public class TaskSchedulerConfiguration {
    internal val tasks = mutableMapOf<TaskManagerName, MutableList<Task>>()
    internal val taskManagers = mutableListOf<(Application) -> TaskManager<*>>()
    public var clock: () -> Instant = { Clock.System.now() }

    @TaskSchedulerDsl
    public fun task(
        taskManagerName: String? = null,
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

    public fun addTaskManager(taskManager: (Application)->TaskManager<*>) {
        taskManagers.add(taskManager)
    }
}


@TaskSchedulerDsl
public class TaskConfiguration {
    public var name: String = ANONYMOUS_TASK_NAME
    public var concurrency: Int = 1
    public var dispatcher: CoroutineDispatcher? = null
    public var task: suspend Application.(DateTime) -> Unit = {}
    public var kronSchedule: (SchedulerBuilder.() -> Unit)? = null

    private companion object {
        const val ANONYMOUS_TASK_NAME = "Anonymous_Task"
    }
}




