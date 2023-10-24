package io.flaxoos.github.knedis

data class RedisReply(
    val type: Int,
    val integer: Long,
    val dval: Double,
    val len: Int,
    val str: String?,
    val vtype: String,
    val elements: Int,
    val element: List<RedisReply>?
)