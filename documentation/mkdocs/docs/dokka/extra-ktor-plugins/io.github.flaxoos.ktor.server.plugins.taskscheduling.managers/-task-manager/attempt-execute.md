---
title: attemptExecute
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers](../index.md)/[TaskManager](index.md)/[attemptExecute](attempt-execute.md)



# attemptExecute



[common]\
abstract suspend fun [attemptExecute](attempt-execute.md)(task: [Task](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md), executionTime: DateTime, concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [TASK_EXECUTION_TOKEN](index.md)?



Try executing the given task at the given execution time with the given concurrency index




