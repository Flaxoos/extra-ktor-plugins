package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.doInfinity
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskLockCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.RedisLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.IntervalTask
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.KronTask
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

public val TaskSchedulerPlugin: ApplicationPlugin<TaskSchedulerConfiguration> = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    application.log.debug("Configuring TaskScheduler")
    val clock = this@createApplicationPlugin.pluginConfig.clock
    val coordinator = with(requireNotNull(this@createApplicationPlugin.pluginConfig.coordinationStrategy) {
        "No coordination strategy configured"
    }) { createCoordinator(application) }.apply {
        // TODO(): make this more elegant
        if (this is TaskLockCoordinator<*>)
            if (this.lockManager is RedisLockManager)
                this.lockManager.clock = clock
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        this.pluginConfig.tasks.forEach { task ->
            application.launch(context = application.coroutineContext.apply {
                task.dispatcher?.let { this + it } ?: this
            }.apply {
                this + CoroutineName(task.name)
            }) {
                when (task) {
                    is IntervalTask -> {
                        delay(task.delay)
                        while (isActive) {
                            application.launch { coordinator.execute(task, clock().toDateTime()) }
                        }
                    }

                    is KronTask -> {
                        task.kronSchedule.doInfinity { dateTime ->
                            application.launch { coordinator.execute(task, dateTime) }
                        }
                    }
                }
            }
        }
    }
}


internal fun Instant.toDateTime(): DateTime = DateTime(toEpochMilliseconds())
