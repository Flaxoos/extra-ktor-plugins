package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

import java.util.LinkedList

actual fun <T>provideQueue(): MutableList<T> = LinkedList()