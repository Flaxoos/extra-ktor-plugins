---
title: RedisLockManager
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis](../index.md)/[RedisLockManager](index.md)



# RedisLockManager



[common]\
class [RedisLockManager](index.md)(val name: [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md), val application: Application, connectionPool: RedisConnectionPool, lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md), connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) : [TaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md)&lt;[RedisTaskLock](../-redis-task-lock/index.md)&gt; 

An implementation of [TaskLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md) using Redis as the lock store



## Constructors


| | |
|---|---|
| [RedisLockManager](-redis-lock-manager.md) | [common]<br>constructor(name: [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md), application: Application, connectionPool: RedisConnectionPool, lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md), connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) |


## Properties


| Name | Summary |
|---|---|
| [application](application.md) | [common]<br>open override val [application](application.md): Application |
| [name](name.md) | [common]<br>open override val [name](name.md): [TaskManagerConfiguration.TaskManagerName](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md) |


## Functions


| Name | Summary |
|---|---|
| [acquireLockKey](acquire-lock-key.md) | [common]<br>open suspend override fun [acquireLockKey](acquire-lock-key.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [RedisTaskLock](../-redis-task-lock/index.md)? |
| [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md) | [common]<br>open suspend override fun [attemptExecute](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [RedisTaskLock](../-redis-task-lock/index.md)? |
| [close](close.md) | [common]<br>open override fun [close](close.md)() |
| [init](init.md) | [common]<br>open suspend override fun [init](init.md)(tasks: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)&gt;) |
| [markExecuted](index.md#-715430314%2FFunctions%2F1182336650) | [common]<br>open suspend override fun [markExecuted](index.md#-715430314%2FFunctions%2F1182336650)(key: [RedisTaskLock](../-redis-task-lock/index.md)) |

