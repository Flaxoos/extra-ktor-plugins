package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.benasher44.uuid.Uuid
import dev.inmo.krontab.KronScheduler
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.server.application.Application
import io.ktor.server.application.log
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@DslMarker
public annotation class TaskSchedulerDsl

@TaskSchedulerDsl
public class TaskSchedulerConfiguration {
    internal val tasks = mutableListOf<Task>()
    public var clock: () -> Instant = { Clock.System.now() }

    public fun task(taskConfiguration: IntervalTask.() -> Unit) {
        tasks.add(IntervalTask().apply(taskConfiguration))
    }

    public fun kronTask(taskConfiguration: KronTask.() -> Unit) {
        tasks.add(KronTask().apply(taskConfiguration).also { checkNotNull(it.kronSchedule) })
    }
}

@TaskSchedulerDsl
public sealed class Task {
    public abstract val id: Uuid
    public abstract var name: String
    public abstract var dispatcher: CoroutineDispatcher?
    public abstract var task: suspend Application.() -> Unit
    public fun executionToken(time: DateTime): TaskExecutionToken = TaskExecutionToken(id, name, time)
}

@TaskSchedulerDsl
public class IntervalTask(override var name: String) : Task() {
    public var schedule: Duration = Duration.INFINITE
    public var delay: Duration = Duration.ZERO
}

@TaskSchedulerDsl
public class KronTask(

    kronSchedule: KronScheduler? = null
) : Task() {
    public var kronSchedule: KronScheduler
        private set

    init {
        checkNotNull(kronSchedule)
        this.kronSchedule = kronSchedule
    }
}

public class TaskExecutionToken(
    public val taskId: Uuid,
    public val taskName: String,
    public val executedAt: DateTime,
    public val short: String = executedAt.toString() // or hashCode()
)

public interface TaskCoordinator {
    public val application: Application
    public suspend fun execute(task: Task, time: DateTime) {
        attemptExecute(task, time)?.let { token ->
            task.task.invoke(application)
            markExecuted(token)
        } ?: application.log.debug("Task execution skipped, denied by $this")
    }

    public suspend fun attemptExecute(task: Task, time: DateTime): TaskExecutionToken?
    public suspend fun markExecuted(token: TaskExecutionToken)
}


