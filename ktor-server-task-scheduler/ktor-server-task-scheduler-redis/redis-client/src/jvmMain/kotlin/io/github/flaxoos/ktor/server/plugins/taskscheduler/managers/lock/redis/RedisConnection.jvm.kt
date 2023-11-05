package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.github.crackthecodeabhi.kreds.args.SetOption
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

class RedisClient(host: String, port: Int) : RedisConnection {

    private val kredsClient = newClient(Endpoint(host, port))

    override suspend fun setNx(
        key: String,
        value: String,
        expiresMs: Long
    ): String? =
        kredsClient.set(key, value, SetOption.Builder().apply {
            pxMilliseconds = expiresMs.toULong()
            nx = true
        }.build()).also {
            logger.trace { "Set: $key, $value, $expiresMs, Result: $it" }
        }

    override suspend fun get(key: String): String? =
        kredsClient.get(key).also {
            logger.trace { "Get: $key, Result: $it" }
        }


    override suspend fun del(key: String): Long =
        kredsClient.del(key).also {
            logger.trace { "Del: $key, Result: $it" }
        }

    override fun close() {
        kredsClient.close()
    }

}

actual fun createRedisConnection(host: String, port: Int): RedisConnection {
    return RedisClient(host, port)
}