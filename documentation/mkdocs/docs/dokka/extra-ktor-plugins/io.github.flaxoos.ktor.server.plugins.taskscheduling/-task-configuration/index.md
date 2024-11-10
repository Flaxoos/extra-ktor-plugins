---
title: TaskConfiguration
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling](../index.md)/[TaskConfiguration](index.md)

# TaskConfiguration

[common]\
class [TaskConfiguration](index.md)

Configuration for a [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)

## Constructors

|                                             |                           |
|---------------------------------------------|---------------------------|
| [TaskConfiguration](-task-configuration.md) | [common]<br>constructor() |

## Properties

| Name                             | Summary                                                                                                                                                                                                      |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrency](concurrency.md)    | [common]<br>var [concurrency](concurrency.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>How many instances of the task should be fired at the same time                   |
| [dispatcher](dispatcher.md)      | [common]<br>var [dispatcher](dispatcher.md): CoroutineDispatcher?<br>What dispatcher should be used to execute the task, if none is provided, the application's dispatcher will be used                      |
| [kronSchedule](kron-schedule.md) | [common]<br>var [kronSchedule](kron-schedule.md): SchedulerBuilder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)?<br>The [kronSchedule](kron-schedule.md) for the task |
| [name](name.md)                  | [common]<br>var [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)<br>The name of the task, should be unique, as it id used to identify the task                |
| [task](task.md)                  | [common]<br>var [task](task.md): suspend Application.(DateTime) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)<br>The actual task logic                                    |

