---
title: acquireLockKey
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock](../index.md)/[TaskLockManager](index.md)/[acquireLockKey](acquire-lock-key.md)



# acquireLockKey



[common]\
abstract suspend fun [acquireLockKey](acquire-lock-key.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [TASK_LOCK](index.md)?



Get permission to execute the task




