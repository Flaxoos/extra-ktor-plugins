package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

interface RedisConnection {
    fun close()
    suspend fun auth(username: String? = null, password: String): Boolean
    suspend fun ping(message: String? = null): String?
    suspend fun setNx(key: String, value: String, expiresMs: Long): String?
    suspend fun get(key: String): String?
    suspend fun del(key: String): Long
    //TODO: implement keepAlive, native supports it out of the box, kreds needs to implement
}

expect fun createRedisConnection(host: String, port: Int): RedisConnection