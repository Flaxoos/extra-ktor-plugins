---
title: RedisTaskLock
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis](../index.md)/[RedisTaskLock](index.md)

# RedisTaskLock

[common]\
@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)

value
class [RedisTaskLock](index.md) : [TaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md)

A [TaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md) implementation for
redis to use as a key

## Types

| Name                             | Summary                                             |
|----------------------------------|-----------------------------------------------------|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Properties

| Name                                     | Summary                                                                                                                                                                                                  |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrencyIndex](concurrency-index.md) | [common]<br>open override val [concurrencyIndex](concurrency-index.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)                                                         |
| [name](name.md)                          | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                                                                            |
| [value](value.md)                        | [common]<br>val [value](value.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)<br>must be unique to a task execution, i.e `$name_$concurrencyIndex at $executionTime` |

