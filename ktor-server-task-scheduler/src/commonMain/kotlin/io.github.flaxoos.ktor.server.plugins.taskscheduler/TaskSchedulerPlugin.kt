package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.doInfinity
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskLockCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.AttributeKey
import korlibs.time.parseLocal
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import korlibs.time.DateFormat as KorDateFormat
import korlibs.time.DateTime as KorDateTime

public val TaskSchedulerPlugin: ApplicationPlugin<TaskSchedulerConfiguration> = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    application.log.debug("Configuring TaskScheduler")
    require(pluginConfig.tasks.distinctBy { it.name }.size == pluginConfig.tasks.size) {
        "Task names must be unique"
    }
    val clock = this@createApplicationPlugin.pluginConfig.clock
    application.attributes.put(ClockAttributeKey, clock)
    val coordinator = with(requireNotNull(this@createApplicationPlugin.pluginConfig.coordinationStrategy) {
        "No coordination strategy configured"
    }) { createCoordinator(application) }

    val insertTasksJob = if (coordinator is TaskLockCoordinator) {
        application.launch { coordinator.init(pluginConfig.tasks) }
    } else null

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.launch {
            insertTasksJob?.join()
            this@createApplicationPlugin.pluginConfig.tasks.forEach { task ->
                application.launch(context = application.coroutineContext.apply {
                    task.dispatcher?.let { this + it } ?: this
                }.apply { this + CoroutineName(task.name) }) {
                    application.startTask(task, coordinator)
                }
            }
        }
    }
}

private suspend fun Application.startTask(
    task: Task,
    coordinator: TaskCoordinator<*>
) {
    withContext(this.coroutineContext) {
        task.kronSchedule.doInfinity { executionTime ->
            launch { coordinator.execute(task, executionTime) }
        }
    }
}

internal fun Application.host() = "Host ${environment.config.property("ktor.deployment.host").getString()}"
internal fun Instant.toDateTimeFormat2(): KorDateTime = KorDateTime(toEpochMilliseconds())
internal fun KorDateTime.format2() = format(KorDateFormat.FORMAT2)
internal fun String.toDateTimeFormat2(): KorDateTime = KorDateFormat.FORMAT2.parseLocal(this)
internal typealias ApplicationClock = () -> Instant

internal fun Application.clock(): Instant = attributes[ClockAttributeKey].invoke()
internal fun Application.clockString(): String = attributes[ClockAttributeKey].invoke().toDateTimeFormat2().format2()
internal val ClockAttributeKey = AttributeKey<ApplicationClock>("ApplicationClock")
