package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import java.util.LinkedList

actual fun <T>provideListForQueue(): MutableList<T> = LinkedList()