---
title: TaskManager
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers](../index.md)/[TaskManager](index.md)



# TaskManager

abstract class [TaskManager](index.md)&lt;[TASK_EXECUTION_TOKEN](index.md) : [TaskExecutionToken](../-task-execution-token/index.md)&gt; : Closeable

#### Inheritors


| |
|---|
| [TaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md) |


## Constructors


| | |
|---|---|
| [TaskManager](-task-manager.md) | [common]<br>constructor() |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |


## Properties


| Name | Summary |
|---|---|
| [application](application.md) | [common]<br>abstract val [application](application.md): Application |
| [name](name.md) | [common]<br>abstract val [name](name.md): [TaskManagerConfiguration.TaskManagerName](../-task-manager-configuration/-task-manager-name/index.md) |


## Functions


| Name | Summary |
|---|---|
| [attemptExecute](attempt-execute.md) | [common]<br>abstract suspend fun [attemptExecute](attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [TASK_EXECUTION_TOKEN](index.md)?<br>Try executing the given task at the given execution time with the given concurrency index |
| [close](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager/index.md#539526881%2FFunctions%2F1182336650) | [common]<br>expect abstract fun [close](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager/index.md#539526881%2FFunctions%2F1182336650)() |
| [init](init.md) | [common]<br>abstract suspend fun [init](init.md)(tasks: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)&gt;)<br>Initialize the [TaskManager](index.md) with the given tasks it manages |
| [markExecuted](mark-executed.md) | [common]<br>abstract suspend fun [markExecuted](mark-executed.md)(key: [TASK_EXECUTION_TOKEN](index.md))<br>Mark this task as, provided a key was acquired |

