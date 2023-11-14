---
title: TaskLockManager
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock](../index.md)/[TaskLockManager](index.md)



# TaskLockManager

abstract class [TaskLockManager](index.md)&lt;[TASK_LOCK](index.md) : [TaskLock](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md)&gt; : [TaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md)&lt;[TASK_LOCK](index.md)&gt; 

#### Inheritors


| |
|---|
| [DatabaseTaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager/index.md) |
| [RedisLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-lock-manager/index.md) |


## Constructors


| | |
|---|---|
| [TaskLockManager](-task-lock-manager.md) | [common]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [application](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/application.md) | [common]<br>abstract val [application](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/application.md): Application |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/name.md) | [common]<br>abstract val [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/name.md): [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md) |


## Functions


| Name | Summary |
|---|---|
| [acquireLockKey](acquire-lock-key.md) | [common]<br>abstract suspend fun [acquireLockKey](acquire-lock-key.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [TASK_LOCK](index.md)?<br>Get permission to execute the task |
| [attemptExecute](attempt-execute.md) | [common]<br>open suspend override fun [attemptExecute](attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [TASK_LOCK](index.md)?<br>Try executing the given task at the given execution time with the given concurrency index |
| [close](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager/index.md#539526881%2FFunctions%2F1182336650) | [common]<br>expect abstract fun [close](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-database-task-lock-manager/index.md#539526881%2FFunctions%2F1182336650)() |
| [init](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/init.md) | [common]<br>abstract suspend fun [init](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/init.md)(tasks: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)&gt;)<br>Initialize the [TaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md) with the given tasks it manages |
| [markExecuted](mark-executed.md) | [common]<br>open suspend override fun [markExecuted](mark-executed.md)(key: [TASK_LOCK](index.md))<br>Mark this task as, provided a key was acquired |

