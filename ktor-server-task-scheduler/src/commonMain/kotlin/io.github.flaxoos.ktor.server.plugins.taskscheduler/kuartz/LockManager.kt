package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz


import io.flaxoos.github.knedis.RedisConnectionPool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

public interface LockManager {
    public suspend fun acquireLock(key: Any): Boolean
    public suspend fun isLocked(key: Any)
    public suspend fun releaseLock(key: Any)
}

//class JobLockStoreManager<Network : Any, Local : Any, Converter, Validator, Updater>(
//    fetch: suspend (key: Uuid) -> Network
//) {
//    init {
//        StoreBuilder
//            .from(
//                fetcher = Fetcher.of {
//                    fetch
//                },
//                sourceOfTruth = SourceOfTruth.of(
//                    reader = {},
//                    writer = {},
//                    delete = {},
//                    deleteAll = {}
//    }
//}

public class RedisLockManager(
    private val connectionPool: RedisConnectionPool,
    private val expiresMs: Int,
    private val timeoutMs: Int,
    public val lockAcquisitionRetryFreqMs: Long = 100,
    private val clock: () -> Instant = { Clock.System.now() },
) : LockManager {

    override suspend fun acquireLock(key: Any): Boolean {
        connectionPool.withConnection { redisConnection ->
            val end = clock() + timeoutMs.milliseconds
            while (clock() < end) {
                if (redisConnection.set(key.toString(), "1", expiresMs) != null) {
                    return@withConnection true
                }
                delay(lockAcquisitionRetryFreqMs)
            }
            false
        } ?: false
    }

    override suspend fun isLocked(key: Any) {
        connectionPool.withConnection {  }
    }

    override suspend fun releaseLock(key: Any) {
        connectionPool.withConnection { redisConnection ->
            redisConnection.del(key.toString())
        }
    }
}

public class RedisJobLockManagerConfig(
    public var host: String = "localhost",
    public var port: Int = 8080,
    public var expiresMs: Long = 100,
    public var timeoutMs: Long = 100,
    public var lockAcquisitionRetryFreqMs: Long = 100,
    public var connectionPoolSize: Int = 10,
    public var clock: () -> Instant = { Clock.System.now() }
)

public class JobLockScheduler(private val jobStore: JobStore, private val lockManager: LockManager) {
    //...
    private suspend fun runJob(job: Job) {
        if (lockManager.acquireLock(job)) {
            // Execute job and release lock afterward
            job.start()
            lockManager.releaseLock(job)
        }
    }
}
