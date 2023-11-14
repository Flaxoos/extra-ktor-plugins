---
title: io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis](index.md)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [RedisLockManager](-redis-lock-manager/index.md) | [common]<br>class [RedisLockManager](-redis-lock-manager/index.md)(val name: [TaskManagerConfiguration.TaskManagerName](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/-task-manager-name/index.md), val application: Application, connectionPool: RedisConnectionPool, lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md), connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) : [TaskLockManager](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md)&lt;[RedisTaskLock](-redis-task-lock/index.md)&gt; <br>An implementation of [TaskLockManager](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager/index.md) using Redis as the lock store |
| [RedisTaskLock](-redis-task-lock/index.md) | [common]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)<br>value class [RedisTaskLock](-redis-task-lock/index.md) : [TaskLock](../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md)<br>A [TaskLock](../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task-lock/index.md) implementation for redis to use as a key |
| [RedisTaskLockManagerConfiguration](-redis-task-lock-manager-configuration/index.md) | [common]<br>class [RedisTaskLockManagerConfiguration](-redis-task-lock-manager-configuration/index.md)(var host: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) = &quot;undefined&quot;, var port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 0, var username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, var password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, var lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = 100, var connectionPoolInitialSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 10, var connectionPoolMaxSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = connectionPoolInitialSize, var connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = 100) : [TaskLockManagerConfiguration](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager-configuration/index.md)&lt;[RedisTaskLock](-redis-task-lock/index.md)&gt; |


## Functions


| Name | Summary |
|---|---|
| [redis](redis.md) | [common]<br>fun [TaskSchedulingConfiguration](../io.github.flaxoos.ktor.server.plugins.taskscheduling/-task-scheduling-configuration/index.md).[redis](redis.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, config: [RedisTaskLockManagerConfiguration](-redis-task-lock-manager-configuration/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Add a [RedisLockManager](-redis-lock-manager/index.md) |

