---
title: DatabaseTaskLockManager
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[DatabaseTaskLockManager](index.md)



# DatabaseTaskLockManager

abstract class [DatabaseTaskLockManager](index.md)&lt;[DB_TASK_LOCK_KEY](index.md) : [DatabaseTaskLock](../-database-task-lock/index.md)&gt; : [TaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md)&lt;[DB_TASK_LOCK_KEY](index.md)&gt; 

An abstract implementation of [TaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md) using a database as the lock store



#### Inheritors


| |
|---|
| JdbcLockManager |
| MongoDBLockManager |


## Constructors


| | |
|---|---|
| [DatabaseTaskLockManager](-database-task-lock-manager.md) | [common]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [application](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/application.md) | [common]<br>abstract val [application](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/application.md): Application |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/name.md) | [common]<br>abstract val [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/name.md): [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md) |


## Functions


| Name | Summary |
|---|---|
| [acquireLockKey](acquire-lock-key.md) | [common]<br>suspend override fun [acquireLockKey](acquire-lock-key.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [DB_TASK_LOCK_KEY](index.md)?<br>Get permission to execute the task |
| [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md) | [common]<br>open suspend override fun [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [DB_TASK_LOCK_KEY](index.md)?<br>Try executing the given task at the given execution time with the given concurrency index |
| [close](index.md#539526881%2FFunctions%2F1182336650) | [common]<br>expect abstract fun [close](index.md#539526881%2FFunctions%2F1182336650)() |
| [init](init.md) | [common]<br>suspend override fun [init](init.md)(tasks: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)&gt;)<br>Initialize the TaskManager with the given tasks it manages |
| [initTaskLockTable](init-task-lock-table.md) | [common]<br>abstract suspend fun [initTaskLockTable](init-task-lock-table.md)()<br>Create the task lock key table in the database |
| [insertTaskLock](insert-task-lock.md) | [common]<br>abstract suspend fun [insertTaskLock](insert-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), taskConcurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)<br>Insert a new task lock key into the database |
| [markExecuted](index.md#-1803818116%2FFunctions%2F1182336650) | [common]<br>open suspend override fun [markExecuted](index.md#-1803818116%2FFunctions%2F1182336650)(key: [DB_TASK_LOCK_KEY](index.md))<br>Mark this task as, provided a key was acquired |
| [updateTaskLock](update-task-lock.md) | [common]<br>abstract suspend fun [updateTaskLock](update-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), executionTime: DateTime): [DB_TASK_LOCK_KEY](index.md)?<br>Try to update the task lock entry in the database, where the key is the combination of task name and concurrency index and execution time different from the given execution time, returning the updated entry or null if none was updated |

