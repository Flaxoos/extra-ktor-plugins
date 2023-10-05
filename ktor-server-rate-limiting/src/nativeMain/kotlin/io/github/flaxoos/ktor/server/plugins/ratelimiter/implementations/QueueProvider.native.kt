package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

actual fun <T>provideListForQueue(): MutableList<T> = ArrayDeque()
