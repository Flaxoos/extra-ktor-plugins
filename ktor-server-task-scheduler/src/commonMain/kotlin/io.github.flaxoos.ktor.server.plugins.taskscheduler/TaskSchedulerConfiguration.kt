package io.github.flaxoos.ktor.server.plugins.taskscheduler

import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration

@DslMarker
annotation class TaskSchedulerDsl

@TaskSchedulerDsl
class TaskSchedulerConfiguration {
    internal var tasks = mutableListOf<Task>()

    fun task(
        name: String,
        schedule: Duration,
        delay: Duration = Duration.ZERO,
        dispatcher: CoroutineDispatcher? = null,
        block: suspend Application.() -> Unit
    ) {
        tasks.add(Task(name, dispatcher, schedule, delay, block))
    }
}

internal class Task(
    val name: String,
    val dispatcher: CoroutineDispatcher?,
    val schedule: Duration,
    val delay: Duration,
    val block: suspend Application.() -> Unit
)