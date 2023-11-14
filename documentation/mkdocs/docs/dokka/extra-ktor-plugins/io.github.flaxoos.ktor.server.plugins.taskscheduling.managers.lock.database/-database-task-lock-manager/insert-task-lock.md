---
title: insertTaskLock
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[DatabaseTaskLockManager](index.md)/[insertTaskLock](insert-task-lock.md)



# insertTaskLock



[common]\
abstract suspend fun [insertTaskLock](insert-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), taskConcurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)



Insert a new task lock key into the database




