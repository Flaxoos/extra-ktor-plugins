package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.github.crackthecodeabhi.kreds.args.SetOption
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.Compiler.command

private val logger = KotlinLogging.logger { }

class RedisClient(host: String, port: Int) : RedisConnection {

    private val kredsClient = newClient(Endpoint(host, port))

    override suspend fun auth(username: String?, password: String): Boolean =
        runCatching { username?.let { kredsClient.auth(it, password) } ?: kredsClient.auth(password) }.map {
            it == "OK"
        }.getOrElse {
            false
        }

    override suspend fun ping(message: String?): String? {
        return kredsClient.ping(message)
    }

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