---
title: Task
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks](../index.md)/[Task](index.md)

# Task

[common]\
data class [Task](index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md),
val dispatcher: CoroutineDispatcher?, val
concurrency: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val kronSchedule: KronScheduler,
val task: suspend Application.(DateTime)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))

## Constructors

|                  |                                                                                                                                                                                                                                                                                                                                                                                            |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Task](-task.md) | [common]<br>constructor(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), dispatcher: CoroutineDispatcher?, concurrency: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), kronSchedule: KronScheduler, task: suspend Application.(DateTime) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)) |

## Properties

| Name                             | Summary                                                                                                                                          |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrency](concurrency.md)    | [common]<br>val [concurrency](concurrency.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)                          |
| [dispatcher](dispatcher.md)      | [common]<br>val [dispatcher](dispatcher.md): CoroutineDispatcher?                                                                                |
| [kronSchedule](kron-schedule.md) | [common]<br>val [kronSchedule](kron-schedule.md): KronScheduler                                                                                  |
| [name](name.md)                  | [common]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                                  |
| [task](task.md)                  | [common]<br>val [task](task.md): suspend Application.(DateTime) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) |

## Functions

| Name                                                                                                                                              | Summary                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrencyRange](concurrency-range.md)                                                                                                          | [common]<br>fun [concurrencyRange](concurrency-range.md)(): [IntRange](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.ranges/-int-range/index.md)                                                                                                                                                                                                                                                                             |
| [toRedisLockKey](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock/-companion/to-redis-lock-key.md) | [common]<br>fun [Task](index.md).[toRedisLockKey](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock/-companion/to-redis-lock-key.md)(executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [RedisTaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock/index.md) |

