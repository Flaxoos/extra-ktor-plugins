---
title: DatabaseTaskLock
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[DatabaseTaskLock](index.md)



# DatabaseTaskLock

interface [DatabaseTaskLock](index.md) : [TaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md)

#### Inheritors


| |
|---|
| JdbcTaskLock |
| MongoDbTaskLock |


## Properties


| Name | Summary |
|---|---|
| [concurrencyIndex](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/concurrency-index.md) | [common]<br>abstract val [concurrencyIndex](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/concurrency-index.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) |
| [lockedAt](locked-at.md) | [common]<br>abstract val [lockedAt](locked-at.md): DateTime |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/name.md) | [common]<br>abstract val [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |

