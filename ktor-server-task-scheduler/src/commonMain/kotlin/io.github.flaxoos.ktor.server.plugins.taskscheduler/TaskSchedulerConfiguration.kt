package io.github.flaxoos.ktor.server.plugins.taskscheduler

import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.buildSchedule
import io.flaxoos.github.knedis.RedisConnectionPool
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.coordinators.TaskLockCoordinator
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.RedisLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@DslMarker
public annotation class TaskSchedulerDsl

@TaskSchedulerDsl
public class TaskSchedulerConfiguration {
    internal val tasks = mutableListOf<Task>()
    public var clock: () -> Instant = { Clock.System.now() }

    @TaskSchedulerDsl
    public fun task(taskConfiguration: TaskConfiguration.() -> Unit) {
        tasks.add(TaskConfiguration().apply(taskConfiguration).also { checkNotNull(it.kronSchedule) }.let {
            Task(
                name = it.name,
                dispatcher = it.dispatcher,
                concurrency = it.concurrency,
                kronSchedule = buildSchedule(it.kronSchedule ?: error("No kron schedule configured")),
                task = it.task
            )
        })
    }

    public var coordinationStrategy: CoordinationStrategy? = null
}


@TaskSchedulerDsl
public class TaskConfiguration {
    public var name: String = ANONYMOUS_TASK_NAME
    public val concurrency: Int = 1
    public var dispatcher: CoroutineDispatcher? = null
    public var task: suspend Application.(DateTime) -> Unit = {}
    public var kronSchedule: (SchedulerBuilder.() -> Unit)? = null

    private companion object {
        const val ANONYMOUS_TASK_NAME = "Anonymous_Task"
    }
}

@TaskSchedulerDsl
public class RedisJobLockManagerConfig(
    public var host: String = "localhost",
    public var port: Int = 8080,
    public var lockExpirationMs: Long = 100,
    public var lockAcquisitionTimeoutMs: Long = 100,
    public var lockAcquisitionRetryFreqMs: Long = 100,
    public var connectionPoolSize: Int = 10,
)

@TaskSchedulerDsl
public abstract class CoordinationStrategy {
    public abstract fun createCoordinator(application: Application): TaskCoordinator<*>
}

public abstract class Lock<LOCK_KEY> : CoordinationStrategy()

public class Redis(private val config: RedisJobLockManagerConfig.() -> Unit) :Lock<Task>() {

    override fun createCoordinator(application: Application): TaskCoordinator<*> = TaskLockCoordinator(
        RedisJobLockManagerConfig().apply(config).let { redisJobLockManagerConfig ->
            RedisLockManager(
                application = application,
                connectionPool = RedisConnectionPool(
                    size = redisJobLockManagerConfig.connectionPoolSize,
                    host = redisJobLockManagerConfig.host,
                    port = redisJobLockManagerConfig.port
                ),
                lockExpirationMs = redisJobLockManagerConfig.lockExpirationMs,
                lockAcquisitionTimeoutMs = redisJobLockManagerConfig.lockAcquisitionTimeoutMs,
                retry = null//  redisJobLockManagerConfig.lockAcquisitionRetryFreqMs,
            )
        },
        application = application
    )
}
//
//private fun <LOCK_KEY> checkExecutionTokenToLockKeyFunction(executionTokenToLockKey: TaskLockKey.() -> LOCK_KEY) {
//    val testToken = {
//        TaskLockKey(
//            taskId = uuid4(),
//            taskName = uuid4().toString(),
//            executedAt = DateTime.now()
//        )
//    }
//    require(testToken().let { listOf(it, it.copy()) }.map { executionTokenToLockKey(it) }
//        .distinct().size == 1) {
//        "executionTokenToLockKey must produce the same key for equal tokens"
//    }
//    val test = listOf(
//        testToken(),
//        testToken().copy(executedAt = DateTime.now().plus(1.milliseconds))
//    )
//    val result = test.map { executionTokenToLockKey(it) }
//    require(
//        result.distinct().size == 2
//    ) {
//        "executionTokenToLockKey must produce different keys for different tokens. test: $test, result: $result"
//    }
//}