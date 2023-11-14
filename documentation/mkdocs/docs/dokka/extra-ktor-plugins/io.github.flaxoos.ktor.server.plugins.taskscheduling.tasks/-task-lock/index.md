---
title: TaskLock
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks](../index.md)/[TaskLock](index.md)



# TaskLock

interface [TaskLock](index.md) : [TaskExecutionToken](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/index.md)

value must be unique to a task execution, i.e name + executionTime



#### Inheritors


| |
|---|
| [DatabaseTaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock/index.md) |
| [RedisTaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock/index.md) |


## Properties


| Name | Summary |
|---|---|
| [concurrencyIndex](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/concurrency-index.md) | [common]<br>abstract val [concurrencyIndex](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/concurrency-index.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/name.md) | [common]<br>abstract val [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |

