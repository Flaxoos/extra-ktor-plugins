---
title: TaskLockManagerConfiguration
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock](../index.md)/[TaskLockManagerConfiguration](index.md)

# TaskLockManagerConfiguration

abstract class [TaskLockManagerConfiguration](index.md)&lt;[TASK_LOCK](index.md)
&gt; : [TaskManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/index.md)
&lt;[TASK_LOCK](index.md)&gt;

#### Inheritors

|                                                                                                                                                                              |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DatabaseTaskLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager-configuration/index.md) |
| [RedisTaskLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock-manager-configuration/index.md)          |

## Constructors

|                                                                     |                           |
|---------------------------------------------------------------------|---------------------------|
| [TaskLockManagerConfiguration](-task-lock-manager-configuration.md) | [common]<br>constructor() |

## Properties

| Name                                                                                                            | Summary                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md) | [common]<br>var [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The name of the task manager, will be used to identify the task manager when assigning tasks to it if none is provided, it will be considered the default one. only one default task manager is allowed. |

## Functions

| Name                                                                                                                                        | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [createTaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/create-task-manager.md) | [common]<br>abstract fun [createTaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/create-task-manager.md)(application: Application): [TaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md)&lt;out [TaskExecutionToken](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-execution-token/index.md)&gt;<br>Create the [TaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md) that this configuration is for |

