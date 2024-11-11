---
title: RedisTaskLockManagerConfiguration
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis](../index.md)/[RedisTaskLockManagerConfiguration](index.md)

# RedisTaskLockManagerConfiguration

[common]\
class [RedisTaskLockManagerConfiguration](index.md)(var
host: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) = &quot;undefined&quot;, var
port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 0, var
username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, var
password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, var
lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = 100, var
connectionPoolInitialSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 10, var
connectionPoolMaxSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) =
connectionPoolInitialSize, var
connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) =
100) : [TaskLockManagerConfiguration](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock/-task-lock-manager-configuration/index.md)
&lt;[RedisTaskLock](../-redis-task-lock/index.md)&gt;

## Constructors

|                                                                                |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|--------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [RedisTaskLockManagerConfiguration](-redis-task-lock-manager-configuration.md) | [common]<br>constructor(host: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) = &quot;undefined&quot;, port: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 0, username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null, lockExpirationMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = 100, connectionPoolInitialSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 10, connectionPoolMaxSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = connectionPoolInitialSize, connectionAcquisitionTimeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = 100) |

## Properties

| Name                                                                                                            | Summary                                                                                                                                                                                                                               |
|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [connectionAcquisitionTimeoutMs](connection-acquisition-timeout-ms.md)                                          | [common]<br>var [connectionAcquisitionTimeoutMs](connection-acquisition-timeout-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>The timeout for trying to get a connection to from the pool     |
| [connectionPoolInitialSize](connection-pool-initial-size.md)                                                    | [common]<br>var [connectionPoolInitialSize](connection-pool-initial-size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>How many connections should the pool have initially                         |
| [connectionPoolMaxSize](connection-pool-max-size.md)                                                            | [common]<br>var [connectionPoolMaxSize](connection-pool-max-size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum number of connections in the pool                                       |
| [host](host.md)                                                                                                 | [common]<br>var [host](host.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)<br>The redis host                                                                                                     |
| [lockExpirationMs](lock-expiration-ms.md)                                                                       | [common]<br>var [lockExpirationMs](lock-expiration-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>For how long the lock should be valid, effectively, the pxMilliseconds for the setNx command |
| [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md) | [common]<br>var [name](../../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager-configuration/name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?                      |
| [password](password.md)                                                                                         | [common]<br>var [password](password.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The redis password                                                                                        |
| [port](port.md)                                                                                                 | [common]<br>var [port](port.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The redis port                                                                                                           |
| [username](username.md)                                                                                         | [common]<br>var [username](username.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The redis username                                                                                        |

## Functions

| Name                                        | Summary                                                                                                                                                  |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| [createTaskManager](create-task-manager.md) | [common]<br>open override fun [createTaskManager](create-task-manager.md)(application: Application): [RedisLockManager](../-redis-lock-manager/index.md) |

