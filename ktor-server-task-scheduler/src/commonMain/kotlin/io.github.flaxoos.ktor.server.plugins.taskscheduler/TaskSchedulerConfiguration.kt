package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.buildSchedule
import io.flaxoos.github.knedis.RedisConnectionPool
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskLockCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.RedisLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.IntervalTask
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.KronTask
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskExecutionToken
import io.ktor.server.application.Application
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

    @TaskSchedulerDsl
    public fun intervalTask(taskConfiguration: IntervalTaskConfiguration.() -> Unit) {
        tasks.add(IntervalTaskConfiguration().apply(taskConfiguration).let {
            IntervalTask(
                id = it.id,
                name = it.name,
                dispatcher = it.dispatcher,
                task = it.task,
                schedule = it.schedule,
                delay = it.delay
            )
        })
    }

    @TaskSchedulerDsl
    public fun kronTask(taskConfiguration: KronTaskConfiguration.() -> Unit) {
        tasks.add(KronTaskConfiguration().apply(taskConfiguration).also { checkNotNull(it.kronSchedule) }.let {
            KronTask(
                id = it.id,
                name = it.name,
                dispatcher = it.dispatcher,
                task = it.task,
                kronSchedule = buildSchedule(it.kronSchedule ?: error("No kron schedule configured"))
            )
        })
    }

    public var coordinationStrategy: CoordinationStrategy? = null
}


@TaskSchedulerDsl
public sealed class TaskConfiguration {
    public var id: Uuid = uuid4()
    public var name: String = ANONYMOUS_TASK_NAME
    public var dispatcher: CoroutineDispatcher? = null
    public var task: suspend Application.() -> Unit = {}

    private companion object {
        const val ANONYMOUS_TASK_NAME = "Anonymous_Task"
    }
}

@TaskSchedulerDsl
public class IntervalTaskConfiguration : TaskConfiguration() {
    public var schedule: Duration = Duration.INFINITE
    public var delay: Duration = Duration.ZERO
}

@TaskSchedulerDsl
public class KronTaskConfiguration : TaskConfiguration() {
    public var kronSchedule: (SchedulerBuilder.() -> Unit)? = null
}

@TaskSchedulerDsl
public class RedisJobLockManagerConfig(
    public var host: String = "localhost",
    public var port: Int = 8080,
    public var expiresMs: Long = 100,
    public var timeoutMs: Long = 100,
    public var lockAcquisitionRetryFreqMs: Long = 100,
    public var connectionPoolSize: Int = 10,
)

@TaskSchedulerDsl
public sealed class CoordinationStrategy {
    public abstract fun createCoordinator(application: Application): TaskCoordinator
    public sealed class Lock<LOCK_KEY>(public val executionTokenToLockKey: TaskExecutionToken.() -> LOCK_KEY) :
        CoordinationStrategy() {
        public class Redis(public val config: RedisJobLockManagerConfig.() -> Unit) : Lock<String>({
            this.toString()
        }) {
            override fun createCoordinator(application: Application): TaskCoordinator = TaskLockCoordinator(
                RedisJobLockManagerConfig().apply(config).let { redisJobLockManagerConfig ->
                    RedisLockManager(
                        connectionPool = RedisConnectionPool(
                            size = redisJobLockManagerConfig.connectionPoolSize,
                            host = redisJobLockManagerConfig.host,
                            port = redisJobLockManagerConfig.port
                        ),
                        expiresMs = redisJobLockManagerConfig.expiresMs,
                        timeoutMs = redisJobLockManagerConfig.timeoutMs,
                        lockAcquisitionRetryFreqMs = redisJobLockManagerConfig.lockAcquisitionRetryFreqMs,
                    )
                }, application, executionTokenToLockKey
            )
        }
    }
}
