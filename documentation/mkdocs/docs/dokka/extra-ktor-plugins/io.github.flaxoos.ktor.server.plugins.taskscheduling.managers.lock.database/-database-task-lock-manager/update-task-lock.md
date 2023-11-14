---
title: updateTaskLock
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[DatabaseTaskLockManager](index.md)/[updateTaskLock](update-task-lock.md)



# updateTaskLock



[common]\
abstract suspend fun [updateTaskLock](update-task-lock.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), executionTime: DateTime): [DB_TASK_LOCK_KEY](index.md)?



Try to update the task lock entry in the database, where the key is the combination of task name and concurrency index and execution time different from the given execution time, returning the updated entry or null if none was updated




