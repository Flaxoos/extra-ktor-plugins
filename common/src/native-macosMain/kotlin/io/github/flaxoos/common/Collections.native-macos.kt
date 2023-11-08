package io.github.flaxoos.common

actual fun <T> queueList(): MutableList<T> = ArrayDeque()
