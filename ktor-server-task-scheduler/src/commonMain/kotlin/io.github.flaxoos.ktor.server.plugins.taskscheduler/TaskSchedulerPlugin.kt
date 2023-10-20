package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.doInfinity
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

val TaskSchedulerPlugin = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    on(MonitoringEvent(ApplicationStarted)) { application ->
        this.pluginConfig.tasks.forEach { task ->

            application.launch(context = application.coroutineContext.apply {
                task.dispatcher?.let { this + it } ?: this
            }.apply {
                task.name?.let { this + CoroutineName(it) } ?: this
            }) {
                when (task) {
                    is IntervalTask -> {
                        delay(task.delay)
                        while (isActive) {
                            task.task.invoke(application)
                            delay(task.schedule)
                        }
                    }

                    is KronTask -> {
                        val taskScheduler = TaskScheduler(task, coordinator)
                        taskScheduler.doInfinity { dateTime ->
                            coordinator.markExecuted(task)
                            task.task.invoke(application)
                        }
                    }
                }
            }
        }
    }
}

object coordinator : TaskCoordinator {
    override fun time(): DateTime {
        TODO("Not yet implemented")
    }

    override fun isTaskExecutedAt(task: Task, time: DateTime): Boolean {
        TODO("Not yet implemented")
    }

    override fun markExecuted(task: Task) {
        TODO("Not yet implemented")
    }
}
