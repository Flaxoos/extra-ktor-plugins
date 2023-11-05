package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import hiredis.redisCommand
import hiredis.redisConnect
import hiredis.redisContext
import hiredis.redisFree
import hiredis.redisReply
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
class RedisClient(private val context: CPointer<redisContext>) : RedisConnection {

    override suspend fun setNx(key: String, value: String, expiresMs: Long): String? =
        command("SET $key $value NX PX ${expiresMs.toULong()}")


    override suspend fun get(key: String): String? =
        command("GET $key")


    override suspend fun del(key: String): Long =
        command("DEL $key")?.toLong() ?: 0L


    override fun close() {
        redisFree(context)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun command(command: String): String? {
        context.let {
            val reply = redisCommand(it, command)?.reinterpret<redisReply>()
            logger.trace { "Command: $command, Reply: $reply" }
            return reply?.pointed?.str?.toKString()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun checkForErrors(): String? {
        return context.pointed.errstr.toKString().takeIf { context.pointed.err != 0 }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createRedisConnection(host: String, port: Int): RedisConnection {
    val context = redisConnect(host, port)
    return if (context?.pointed?.err == 0) {
        RedisClient(context)
    } else throw IllegalStateException("Could not create redis connection, null pointer returned.")
}