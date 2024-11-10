---
title: JdbcLockManager
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[JdbcLockManager](index.md)

# JdbcLockManager

[jvm]\
class [JdbcLockManager](index.md)(val
name: [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md),
val application: Application, database: Database,
taskLockTable: [ExposedTaskLockTable](../-exposed-task-lock-table/index.md) =
DefaultTaskLockTable) : [DatabaseTaskLockManager](../-database-task-lock-manager/index.md)
&lt;[JdbcTaskLock](../-jdbc-task-lock/index.md)&gt;

An implementation of [DatabaseTaskLockManager](../-database-task-lock-manager/index.md) using JDBC and Exposed as the
lock store The manager will take care of generating the lock table using the SchemaUtils and the DefaultTaskLockTable.
the schema utils should handle the case where the table already exists. TODO: test this Alternatively, you can use
implement the [ExposedTaskLockTable](../-exposed-task-lock-table/index.md) yourself and provide it instead

## Constructors

|                                          |                                                                                                                                                                                                                                                                                                                                                       |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [JdbcLockManager](-jdbc-lock-manager.md) | [jvm]<br>constructor(name: [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md), application: Application, database: Database, taskLockTable: [ExposedTaskLockTable](../-exposed-task-lock-table/index.md) = DefaultTaskLockTable) |

## Properties

| Name                          | Summary                                                                                                                                                                                                             |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [application](application.md) | [jvm]<br>open override val [application](application.md): Application                                                                                                                                               |
| [name](name.md)               | [jvm]<br>open override val [name](name.md): [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md) |

## Functions

| Name                                                                                                                             | Summary                                                                                                                                                                                                                                                                                                                                                                                                                               |
|----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [acquireLockKey](../-database-task-lock-manager/acquire-lock-key.md)                                                             | [jvm]<br>suspend override fun [acquireLockKey](../-database-task-lock-manager/acquire-lock-key.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [JdbcTaskLock](../-jdbc-task-lock/index.md)?                                                                  |
| [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md) | [jvm]<br>open suspend override fun [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [JdbcTaskLock](../-jdbc-task-lock/index.md)? |
| [close](close.md)                                                                                                                | [jvm]<br>open override fun [close](close.md)()                                                                                                                                                                                                                                                                                                                                                                                        |
| [init](../-database-task-lock-manager/init.md)                                                                                   | [jvm]<br>suspend override fun [init](../-database-task-lock-manager/init.md)(tasks: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)&gt;)                                                                                                                                                            |
| [initTaskLockTable](init-task-lock-table.md)                                                                                     | [jvm]<br>open suspend override fun [initTaskLockTable](init-task-lock-table.md)()                                                                                                                                                                                                                                                                                                                                                     |
| [insertTaskLock](insert-task-lock.md)                                                                                            | [jvm]<br>open suspend override fun [insertTaskLock](insert-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), taskConcurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)                                                                             |
| [markExecuted](index.md#1666649552%2FFunctions%2F1975120172)                                                                     | [jvm]<br>open suspend override fun [markExecuted](index.md#1666649552%2FFunctions%2F1975120172)(key: [JdbcTaskLock](../-jdbc-task-lock/index.md))                                                                                                                                                                                                                                                                                     |
| [updateTaskLock](update-task-lock.md)                                                                                            | [jvm]<br>open suspend override fun [updateTaskLock](update-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), executionTime: DateTime): [JdbcTaskLock](../-jdbc-task-lock/index.md)?                                                                                            |

