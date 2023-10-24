package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.doInfinity
import io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz.LockManager
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

public val TaskSchedulerPlugin: ApplicationPlugin<TaskSchedulerConfiguration> = createApplicationPlugin(
    name = "TaskScheduler",
    createConfiguration = ::TaskSchedulerConfiguration
) {
    val clock = this@createApplicationPlugin.pluginConfig.clock
    on(MonitoringEvent(ApplicationStarted)) { application ->
        this.pluginConfig.tasks.forEach { task ->
            val coordinator = TaskLockCoordinator()

            application.launch(context = application.coroutineContext.apply {
                task.dispatcher?.let { this + it } ?: this
            }.apply {
                task.name?.let { this + CoroutineName(it) } ?: this
            }) {
                when (task) {
                    is IntervalTask -> {
                        delay(task.delay)
                        while (isActive) {
                            coordinator.execute(task, clock().toDateTime())
                        }
                    }

                    is KronTask -> {
                        task.kronSchedule.doInfinity { dateTime ->
                            coordinator.execute(task, dateTime)
                        }
                    }
                }
            }
        }
    }
}

public class TaskLockCoordinator(
    public val lockManager: LockManager,
    override val application: Application
    public val serialize: TaskExecutionToken.() -> String,
) : TaskCoordinator {

    public override suspend fun attemptExecute(task: Task, time: DateTime): TaskExecutionToken? =
        task.executionToken(time).let { token ->
            if (lockManager.acquireLock(token.serialize())) token else null
        }

    override suspend fun markExecuted(token: TaskExecutionToken) {
        lockManager.releaseLock(token.serialize())
    }

}

internal fun Instant.toDateTime(): DateTime = DateTime(toEpochMilliseconds())
