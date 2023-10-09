package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import dev.inmo.krontab.KronScheduler
import dev.inmo.krontab.builder.buildSchedule
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Instant
import kotlin.time.Duration

@DslMarker
annotation class TaskSchedulerDsl

@TaskSchedulerDsl
class TaskSchedulerConfiguration {
    internal val tasks = mutableListOf<Task>()

    fun task(
        taskConfiguration: IntervalTask.() -> Unit,
    ) {
        tasks.add(IntervalTask().apply(taskConfiguration))
    }

    fun kronTask(
        taskConfiguration: KronTask.() -> Unit
    ) {
        tasks.add(KronTask().apply(taskConfiguration).also { checkNotNull(it.kronSchedule) })
    }
}

fun m() {
    TaskSchedulerConfiguration().apply {
        kronTask {
            buildSchedule {
                seconds {
                    from(0) to 1
                }
            }
        }
    }
}

@TaskSchedulerDsl
sealed class Task {
    val id: Uuid = uuid4()
    var name: String? = null
    var dispatcher: CoroutineDispatcher? = null
    var task: suspend Application.() -> Unit = {}
}

@TaskSchedulerDsl
class IntervalTask : Task() {
    var schedule: Duration = Duration.INFINITE
    var delay: Duration = Duration.ZERO
}

@TaskSchedulerDsl
class KronTask(
    kronSchedule: KronScheduler? = null
) : Task() {
    var kronSchedule: KronScheduler
        private set

    init {
        checkNotNull(kronSchedule)
        this.kronSchedule = kronSchedule
    }
}

class TaskScheduler(
    val task: KronTask,
    val coordinator: TaskCoordinator
) : KronScheduler {

    override suspend fun next(relatively: DateTime): DateTime? {
        return if (coordinator.isTaskExecutedAt(task, relatively)) null else
            task.kronSchedule.next(relatively)
    }
}

interface TaskCoordinator {
    fun time(): DateTime
    fun isTaskExecutedAt(task: Task, time: DateTime): Boolean
    fun markExecuted(task: Task)
}
