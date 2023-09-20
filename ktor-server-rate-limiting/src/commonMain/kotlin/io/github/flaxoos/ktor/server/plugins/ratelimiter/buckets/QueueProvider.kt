package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

expect fun <T> provideQueue(): MutableList<T>
