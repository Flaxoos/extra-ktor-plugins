---
title: JdbcJobLockManagerConfiguration
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[JdbcJobLockManagerConfiguration](index.md)

# JdbcJobLockManagerConfiguration

[jvm]\
class [JdbcJobLockManagerConfiguration](index.md) : [DatabaseTaskLockManagerConfiguration](../-database-task-lock-manager-configuration/index.md)
&lt;[JdbcTaskLock](../-jdbc-task-lock/index.md)&gt;

## Constructors

|                                                                            |                        |
|----------------------------------------------------------------------------|------------------------|
| [JdbcJobLockManagerConfiguration](-jdbc-job-lock-manager-configuration.md) | [jvm]<br>constructor() |

## Properties

| Name                                                                                                            | Summary                                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [database](database.md)                                                                                         | [jvm]<br>var [database](database.md): Database                                                                                                                                                                |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md) | [jvm]<br>var [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? |

## Functions

| Name                                        | Summary                                                                                                                                             |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| [createTaskManager](create-task-manager.md) | [jvm]<br>open override fun [createTaskManager](create-task-manager.md)(application: Application): [JdbcLockManager](../-jdbc-lock-manager/index.md) |

