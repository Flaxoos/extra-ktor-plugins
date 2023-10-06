package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import java.util.LinkedList

actual fun <T> provideListForQueue(): MutableList<T> = LinkedList()
