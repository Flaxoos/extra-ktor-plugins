package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.github.flaxoos.common.queueList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException

private val logger = KotlinLogging.logger { }

private const val DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_MS = 100L

/**
 * Redis Connection Pool
 *
 * Supplies [RedisConnection]s to the given host and port
 *
 * @property initialConnectionCount the number of initial connections
 * @property maxConnectionCount the maximum number of connections
 * @property host the host
 * @property port the port
 */
class RedisConnectionPool(
    private val initialConnectionCount: Int,
    private val maxConnectionCount: Int = initialConnectionCount,
    private val host: String,
    private val port: Int,
    private val username: String? = null,
    private val password: String? = null
) {
    private val pool = queueList<RedisConnection>().apply {
        addAll((0.until(this@RedisConnectionPool.initialConnectionCount)).mapNotNull {
            createRedisConnection(host, port)
        })
    }
    private var used = 0
    private val mutex = Mutex()

    init {
        require(initialConnectionCount > 0) {
            "initialConnectionCount must be greater than 0"
        }
        require(maxConnectionCount >= initialConnectionCount) {
            "maxConnectionCount must be greater than or equal to initialConnectionCount"
        }
        runCatching {
            runBlocking {
                withConnection { connection ->
                    password?.let { connection.auth(username, password) } ?: connection.ping(
                        "test"
                    ).let { if (it != "test") throw IOException("Ping failed") }
                }
            }
        }.onFailure {
            throw IOException(
                "Could not connect to Redis with the given host:$host and port:$port",
                it
            )
        }
    }

    /**
     * Executes the given [block] with a [RedisConnection]. The operation is retried up to [connectionAcquisitionTimeoutMs]
     */
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
        return mutex.withLock {
            if (pool.isEmpty()) {
                if (used >= maxConnectionCount) {
                    logger.debug { "Exceeded max connection count. Pool size: ${pool.size}, Used: $used" }
                    null
                } else {
                    used++
                    createRedisConnection(host, port).also {
                        logger.debug { "All initial connections used, creating new connection. Pool size: ${pool.size}, Used: $used" }
                    }
                }
            } else pool.last().also {
                logger.debug { "Providing existing connection. Pool size: ${pool.size}, Used: $used" }
                used++
                pool.removeLast()
            }
        }
    }

    private suspend fun returnConnection(conn: RedisConnection) {
        mutex.withLock {
            used--
            pool.add(conn).also {
                logger.debug { "Returning connection to pool. Pool size: ${pool.size}, Used: $used" }
            }
        }
    }

    suspend fun closeAll() {
        mutex.withLock {
            pool.forEach {
                it.close()
            }
        }
    }
}


