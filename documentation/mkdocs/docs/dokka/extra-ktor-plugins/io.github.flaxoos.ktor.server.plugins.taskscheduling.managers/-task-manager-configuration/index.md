---
title: TaskManagerConfiguration
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers](../index.md)/[TaskManagerConfiguration](index.md)



# TaskManagerConfiguration

abstract class [TaskManagerConfiguration](index.md)&lt;[TASK_EXECUTION_TOKEN](index.md)&gt;

Configuration for [TaskManager](../-task-manager/index.md)



#### Inheritors


| |
|---|
| [TaskLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager-configuration/index.md) |


## Constructors


| | |
|---|---|
| [TaskManagerConfiguration](-task-manager-configuration.md) | [common]<br>constructor() |


## Types


| Name | Summary |
|---|---|
| [TaskManagerName](-task-manager-name/index.md) | [common]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)<br>value class [TaskManagerName](-task-manager-name/index.md)(val value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)) |


## Properties


| Name | Summary |
|---|---|
| [name](name.md) | [common]<br>var [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The name of the task manager, will be used to identify the task manager when assigning tasks to it if none is provided, it will be considered the default one. only one default task manager is allowed. |


## Functions


| Name | Summary |
|---|---|
| [createTaskManager](create-task-manager.md) | [common]<br>abstract fun [createTaskManager](create-task-manager.md)(application: Application): [TaskManager](../-task-manager/index.md)&lt;out [TaskExecutionToken](../-task-execution-token/index.md)&gt;<br>Create the [TaskManager](../-task-manager/index.md) that this configuration is for |

