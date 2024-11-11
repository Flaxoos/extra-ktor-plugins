---
title: MongoDBJobLockManagerConfiguration
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[MongoDBJobLockManagerConfiguration](index.md)

# MongoDBJobLockManagerConfiguration

[jvm]\
class [MongoDBJobLockManagerConfiguration](index.md) : [DatabaseTaskLockManagerConfiguration](../-database-task-lock-manager-configuration/index.md)
&lt;[MongoDbTaskLock](../-mongo-db-task-lock/index.md)&gt;

## Constructors

|                                                                                    |                        |
|------------------------------------------------------------------------------------|------------------------|
| [MongoDBJobLockManagerConfiguration](-mongo-d-b-job-lock-manager-configuration.md) | [jvm]<br>constructor() |

## Properties

| Name                                                                                                            | Summary                                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [client](client.md)                                                                                             | [jvm]<br>var [client](client.md): MongoClient                                                                                                                                                                 |
| [databaseName](database-name.md)                                                                                | [jvm]<br>var [databaseName](database-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                                                                                 |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md) | [jvm]<br>var [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? |

## Functions

| Name                                        | Summary                                                                                                                                                     |
|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [createTaskManager](create-task-manager.md) | [jvm]<br>open override fun [createTaskManager](create-task-manager.md)(application: Application): [MongoDBLockManager](../-mongo-d-b-lock-manager/index.md) |

