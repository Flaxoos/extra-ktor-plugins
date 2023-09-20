package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

actual fun <T>provideQueue(): MutableList<T> = ArrayDeque()