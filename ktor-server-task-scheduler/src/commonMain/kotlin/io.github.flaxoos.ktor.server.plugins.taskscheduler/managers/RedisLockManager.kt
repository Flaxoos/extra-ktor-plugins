package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers

import io.flaxoos.github.knedis.RedisConnectionPool
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.milliseconds

public class RedisLockManager(
    private val connectionPool: RedisConnectionPool,
    private val expiresMs: Long,
    private val timeoutMs: Long,
    private val lockAcquisitionRetryFreqMs: Long = 100,
) : LockManager<String> {

    internal var clock: () -> Instant by Delegates.notNull()

    override suspend fun acquireLock(key: String): Boolean =
        connectionPool.withConnection { redisConnection ->
            val end = clock() + timeoutMs.milliseconds
            while (clock() < end) {
                if (redisConnection.set(key, "1", expiresMs) != null) {
                    return@withConnection true
                }
                delay(lockAcquisitionRetryFreqMs)
            }
            false
        } ?: false

    override suspend fun isLocked(key: String) {
        connectionPool.withConnection { redisConnection ->
            redisConnection.get(key)
        }
    }

    override suspend fun releaseLock(key: String) {
        connectionPool.withConnection { redisConnection ->
            redisConnection.del(key)
        }
    }
}

