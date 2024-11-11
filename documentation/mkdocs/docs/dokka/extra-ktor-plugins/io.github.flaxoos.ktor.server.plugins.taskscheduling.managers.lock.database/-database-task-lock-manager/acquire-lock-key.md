---
title: acquireLockKey
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[DatabaseTaskLockManager](index.md)/[acquireLockKey](acquire-lock-key.md)

# acquireLockKey

[common]\
suspend override fun [acquireLockKey](acquire-lock-key.md)(
task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime,
concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [DB_TASK_LOCK_KEY](index.md)?

Get permission to execute the task




