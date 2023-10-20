package io.flaxoos.github.knedis

import hiredis.redisCommand
import hiredis.redisConnect
import hiredis.redisContext
import hiredis.redisFree
import hiredis.redisReply
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
class RedisClient(val host: String, val port: Int) {

    internal var context: CPointer<redisContext>? = null

    fun connect(): Boolean {
        context = redisConnect(host, port)
        return context?.pointed?.err == 0
    }

    fun disconnect() {
        // Cleanup resources and disconnect...
        context?.let { redisFree(it) }
    }
}

fun redisClient(host: String, port: Int): RedisClient {
    return RedisClient(host, port)
}

@OptIn(ExperimentalForeignApi::class)
fun RedisClient.command(command: String): String? {
    context?.let {
        val reply = redisCommand(it, command)?.reinterpret<redisReply>()
        return reply?.pointed?.str?.toKString()
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
fun RedisClient.checkForErrors(): String? {
    return context?.pointed?.errstr?.toKString().takeIf { context?.pointed?.err != 0 }
}