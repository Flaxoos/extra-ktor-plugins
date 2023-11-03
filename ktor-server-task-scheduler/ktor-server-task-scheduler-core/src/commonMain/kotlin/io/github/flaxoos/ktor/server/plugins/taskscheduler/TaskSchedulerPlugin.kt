package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.doInfinity
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import korlibs.time.parseLocal
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import korlibs.time.DateFormat as KorDateFormat
import korlibs.time.DateTime as KorDateTime

public val TaskSchedulerPlugin: ApplicationPlugin<TaskSchedulerConfiguration> = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    application.log.debug("Configuring TaskScheduler")

    checkUniqueTaskNames()

    val taskManagers = createTaskManagers()

    checkTaskMangerAssignments(taskManagers)

    val taskManagerInits = initializeTaskManagers(taskManagers)

    on(MonitoringEvent(ApplicationStarted)) { application ->
        taskManagers.forEach { manager ->
            taskManagerInits[manager]?.let { tasks ->
                application.launch {
                    tasks.await().forEach { task ->
                        manager.startTask(task)
                    }
                }
            }
        }
    }
}

private fun PluginBuilder<TaskSchedulerConfiguration>.initializeTaskManagers(
    taskManagers: List<TaskManager<*>>
) = taskManagers.mapNotNull { manager ->
    pluginConfig.tasks[manager.name]?.let { tasks ->
        manager to application.async {
            manager.init(tasks)
            tasks.toList()
        }
    } ?: error("Configuration verification did not check for missing task managers assigned to tasks, this is a bug")
}.toMap()

private fun PluginBuilder<TaskSchedulerConfiguration>.checkTaskMangerAssignments(
    taskManagers: List<TaskManager<*>>
) {
    with(pluginConfig.tasks.filter { it.key !in taskManagers.map { it.name } }) {
        require(isEmpty()) {
            "Bad configuration: The following tasks manager names were assigned tasks but were not created: ${
            this.toList().joinToString {
                "${it.first}: ${it.second.joinToString() { task -> task.name }}}"
            }
            }"
        }
    }
}

private fun PluginBuilder<TaskSchedulerConfiguration>.createTaskManagers(): List<TaskManager<*>> {
    val taskManagers = pluginConfig.taskManagers.map { createTaskManager -> createTaskManager(application) }
    require(taskManagers.isNotEmpty()) { "No task managers were configured" }
    return taskManagers
}

private fun PluginBuilder<TaskSchedulerConfiguration>.checkUniqueTaskNames() {
    with(pluginConfig.tasks.values.flatten().groupingBy { it.name }.eachCount()) {
        require(all { it.value == 1 }) {
            "Bad configuration: Task names must be unique, but the following tasks names are repeated: ${this.keys.joinToString()}"
        }
    }
}

private fun TaskManager<*>.startTask(task: Task) {
    application.launch(
        context = application.coroutineContext.apply {
            task.dispatcher?.let { this + it } ?: this
        }.apply { this + CoroutineName(task.name) }
    ) {
        task.kronSchedule.doInfinity { executionTime ->
            execute(task, executionTime)
        }
    }
}

public fun Application.host(): String = "Host ${environment.config.property("ktor.deployment.host").getString()}"
public fun KorDateTime.format2(): String = format(KorDateFormat.FORMAT2)
public fun String.format2ToDateTime(): KorDateTime = KorDateFormat.FORMAT2.parseLocal(this)
