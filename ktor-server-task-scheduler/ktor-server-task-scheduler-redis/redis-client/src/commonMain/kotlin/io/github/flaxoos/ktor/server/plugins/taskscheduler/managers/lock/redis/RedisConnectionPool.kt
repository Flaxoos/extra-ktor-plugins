package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.github.flaxoos.common.queueList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private val logger = KotlinLogging.logger { }

private const val DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_MS = 100L

class RedisConnectionPool(private val size: Int, private val host: String, private val port: Int) {
    private val pool = queueList<RedisConnection>().apply {
        addAll((0.until(this@RedisConnectionPool.size)).mapNotNull {
            createRedisConnection(host, port)
        })
    }
    private val mutex = Mutex()

    suspend fun <T> withConnection(
        connectionAcquisitionTimeoutMs: Long = DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_MS,
        block: suspend (RedisConnection) -> T
    ): T? {
        return withTimeoutOrNull(connectionAcquisitionTimeoutMs) {
            var connection = getConnection()
            while (connection == null) {
                delay(10)
                connection = getConnection()
            }
            connection
        }?.let { connection ->
            block(connection).also { returnConnection(connection) }
        } ?: run {
            logger.debug { "Could not acquire connection within $connectionAcquisitionTimeoutMs ms" }
            null
        }
    }

    private suspend fun getConnection(): RedisConnection? {
        return mutex.withLock { if (pool.isEmpty()) null else pool.last().also { pool.removeLast() } }
    }

    private suspend fun returnConnection(conn: RedisConnection) {
        mutex.withLock { pool.add(conn) }
    }

    suspend fun closeAll() {
        mutex.withLock {
            pool.forEach {
                it.close()
            }
        }
    }
}


