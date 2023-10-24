package io.flaxoos.github.knedis

import io.github.flaxoos.common.queueList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RedisConnectionPool(private val size: Int, private val host: String, private val port: Int) {
    private val pool = queueList<RedisConnection>().apply {
        addAll((0..this@RedisConnectionPool.size).mapNotNull {
            createRedisConnection(host, port)
        })
    }
    private val mutex = Mutex()

    suspend fun <T> withConnection(block: suspend (RedisConnection) -> T): T? {
        return getConnection()?.let {
            val result = block(it)
            returnConnection(it)
            result
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

interface RedisConnection {
    fun close()
    suspend fun set(key: String, value: String, expiresMs: Long): String?
    suspend fun get(key: String): String?
    suspend fun del(key: String): Long
}

expect fun createRedisConnection(host: String, port: Int): RedisConnection?


