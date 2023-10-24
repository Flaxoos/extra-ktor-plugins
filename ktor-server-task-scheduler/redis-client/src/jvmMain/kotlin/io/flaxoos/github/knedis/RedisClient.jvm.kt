package io.flaxoos.github.knedis

import io.github.crackthecodeabhi.kreds.args.SetOption
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient

class RedisClient(host: String, port: Int) : RedisConnection {

    private val kredsClient = newClient(Endpoint(host, port))

    override suspend fun set(
        key: String,
        value: String,
        expiresMs: Long
    ): String? =
        kredsClient.set(key, value, SetOption.Builder().apply {
            pxMilliseconds = expiresMs.toULong()
            nx = true
        }.build())

    override suspend fun get(key: String): String? =
        kredsClient.get(key)


    override suspend fun del(key: String): Long =
        kredsClient.del(key)

    override fun close() {
        kredsClient.close()
    }

}

actual fun createRedisConnection(host: String, port: Int): RedisConnection? {
    return RedisClient(host, port)
}