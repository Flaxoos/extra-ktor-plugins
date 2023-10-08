package io.github.flaxoos.ktor.server.plugins.taskscheduler

import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val TaskScheduler = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    on(MonitoringEvent(ApplicationStarted)) { application ->
        this.pluginConfig.tasks.forEach { task ->
            application.launch {
                with(task) {
                    delay(delay)
                    while (isActive) {
                        task.block.invoke(application)
                    }
                }
            }
        }
    }
}