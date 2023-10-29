package io.flaxoos.github.knedis

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

interface RedisConnection {
    fun close()
    suspend fun set(key: String, value: String, expiresMs: Long): String?
    suspend fun get(key: String): String?
    suspend fun del(key: String): Long
}

expect fun createRedisConnection(host: String, port: Int): RedisConnection?