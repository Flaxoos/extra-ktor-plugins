---
title: TaskSchedulingConfiguration
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling](../index.md)/[TaskSchedulingConfiguration](index.md)



# TaskSchedulingConfiguration



[common]\
open class [TaskSchedulingConfiguration](index.md)

Configuration for [TaskScheduling](../-task-scheduling.md)



## Constructors


| | |
|---|---|
| [TaskSchedulingConfiguration](-task-scheduling-configuration.md) | [common]<br>constructor() |


## Functions


| Name | Summary |
|---|---|
| [addTaskManager](add-task-manager.md) | [common]<br>fun [addTaskManager](add-task-manager.md)(taskManagerConfiguration: [TaskManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/index.md)&lt;*&gt;) |
| [jdbc](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/jdbc.md) | [jvm]<br>fun [TaskSchedulingConfiguration](index.md).[jdbc](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/jdbc.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, config: [JdbcJobLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-jdbc-job-lock-manager-configuration/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Add a [JdbcLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-jdbc-lock-manager/index.md) |
| [mongoDb](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/mongo-db.md) | [jvm]<br>fun [TaskSchedulingConfiguration](index.md).[mongoDb](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/mongo-db.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, config: [MongoDBJobLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-mongo-d-b-job-lock-manager-configuration/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Add a [MongoDBLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database/-mongo-d-b-lock-manager/index.md) |
| [redis](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/redis.md) | [common]<br>fun [TaskSchedulingConfiguration](index.md).[redis](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/redis.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, config: [RedisTaskLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-task-lock-manager-configuration/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Add a [RedisLockManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis/-redis-lock-manager/index.md) |
| [task](task.md) | [common]<br>fun [task](task.md)(taskManagerName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, taskConfiguration: [TaskConfiguration](../-task-configuration/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Add a task to be managed by a [TaskManager](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md) with the given name or the default one if no name is provided and a default task manager has been configured |

