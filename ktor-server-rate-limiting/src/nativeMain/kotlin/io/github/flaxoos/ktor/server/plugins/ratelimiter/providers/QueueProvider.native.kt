package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

actual fun <T>provideListForQueue(): MutableList<T> = ArrayDeque()