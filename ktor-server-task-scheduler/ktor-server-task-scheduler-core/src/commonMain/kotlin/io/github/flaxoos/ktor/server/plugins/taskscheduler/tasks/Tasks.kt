package io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks

import dev.inmo.krontab.KronScheduler
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher

@TaskSchedulerDsl
public data class Task(
    public val name: String,
    public val dispatcher: CoroutineDispatcher?,
    public val concurrency: Int,
    public val kronSchedule: KronScheduler,
    public val task: suspend Application.(DateTime) -> Unit
) {
    public fun concurrencyRange(): IntRange = 1..concurrency
}

/**
 * value must be unique to a task execution, i.e name + executionTime
 */
public interface TaskLockKey
