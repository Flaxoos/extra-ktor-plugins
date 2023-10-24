package io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks

import com.benasher44.uuid.Uuid
import dev.inmo.krontab.KronScheduler
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration

@TaskSchedulerDsl
public sealed class Task {
    public abstract val id: Uuid
    public abstract val name: String
    public abstract val dispatcher: CoroutineDispatcher?
    public abstract val task: suspend Application.() -> Unit
    public fun executionToken(time: DateTime): TaskExecutionToken = TaskExecutionToken(id, name, time)
}

@TaskSchedulerDsl
public class IntervalTask(
    override val id: Uuid,
    override var name: String,
    override val dispatcher: CoroutineDispatcher?,
    override val task: suspend Application.() -> Unit,
    public val schedule: Duration,
    public val delay: Duration
) : Task()

@TaskSchedulerDsl
public class KronTask(
    override val id: Uuid,
    override var name: String,
    override val dispatcher: CoroutineDispatcher?,
    override val task: suspend Application.() -> Unit,
    public val kronSchedule: KronScheduler
) : Task()

public class TaskExecutionToken(
    public val taskId: Uuid,
    public val taskName: String,
    public val executedAt: DateTime,
    public val shorten: TaskExecutionToken.() -> String = { this.toString() }// or hashCode()?
)