package io.github.flaxoos.ktor.server.plugins.taskscheduling

import dev.inmo.krontab.doInfinity
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Task scheduler plugin
 *
 * Provides scheduling capabilities for a given set of scheduled tasks.
 * The tasks are managed by some implementation of [TaskManager], that is responsible for coordinating the execution
 * of the tasks across the different instances of the application.
 */
public val TaskScheduling: ApplicationPlugin<TaskSchedulingConfiguration> =
    createApplicationPlugin(
        name = "TaskScheduling",
        createConfiguration = ::TaskSchedulingConfiguration,
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

private fun PluginBuilder<TaskSchedulingConfiguration>.initializeTaskManagers(taskManagers: List<TaskManager<*>>) =
    taskManagers
        .mapNotNull { manager ->
            pluginConfig.tasks[manager.name]?.let { tasks ->
                manager to
                    application.async {
                        manager.init(tasks)
                        tasks.toList()
                    }
            } ?: error("Configuration verification did not check for missing task managers assigned to tasks, this is a bug")
        }.toMap()

private fun PluginBuilder<TaskSchedulingConfiguration>.checkTaskMangerAssignments(taskManagers: List<TaskManager<*>>) {
    with(pluginConfig.tasks.filter { it.key !in taskManagers.map { taskManager -> taskManager.name } }) {
        require(isEmpty()) {
            "Bad configuration: The following tasks manager names were assigned tasks but were not created: " +
                toList().joinToString {
                    "${it.first}: ${it.second.joinToString { task -> task.name }}}"
                }
        }
    }
}

private fun PluginBuilder<TaskSchedulingConfiguration>.createTaskManagers(): List<TaskManager<*>> {
    val taskManagers = pluginConfig.taskManagers.map { createTaskManager -> createTaskManager(application) }
    require(taskManagers.isNotEmpty()) { "No task managers were configured" }
    return taskManagers
}

private fun PluginBuilder<TaskSchedulingConfiguration>.checkUniqueTaskNames() {
    with(
        pluginConfig.tasks.values
            .flatten()
            .groupingBy { it.name }
            .eachCount(),
    ) {
        require(all { it.value == 1 }) {
            "Bad configuration: Task names must be unique, but the following tasks names are repeated: ${this.keys.joinToString()}"
        }
    }
}

private fun TaskManager<*>.startTask(task: Task) {
    application.launch(
        context =
            application.coroutineContext
                .apply {
                    task.dispatcher?.let { this + it } ?: this
                }.apply { this + CoroutineName(task.name) },
    ) {
        task.kronSchedule.doInfinity { executionTime ->
            execute(task, executionTime)
        }
    }
}
