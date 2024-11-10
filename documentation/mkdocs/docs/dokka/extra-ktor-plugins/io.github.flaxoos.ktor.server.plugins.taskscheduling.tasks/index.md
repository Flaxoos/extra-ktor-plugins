---
title: io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks](index.md)

# Package-level declarations

## Types

| Name                            | Summary                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Task](-task/index.md)          | [common]<br>data class [Task](-task/index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), val dispatcher: CoroutineDispatcher?, val concurrency: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val kronSchedule: KronScheduler, val task: suspend Application.(DateTime) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)) |
| [TaskLock](-task-lock/index.md) | [common]<br>interface [TaskLock](-task-lock/index.md) : [TaskExecutionToken](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/index.md)<br>value must be unique to a task execution, i.e name + executionTime                                                                                                                                                                                  |

