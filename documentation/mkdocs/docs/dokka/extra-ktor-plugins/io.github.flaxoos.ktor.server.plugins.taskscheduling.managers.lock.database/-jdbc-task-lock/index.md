---
title: JdbcTaskLock
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[JdbcTaskLock](index.md)

# JdbcTaskLock

[jvm]\
class [JdbcTaskLock](index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md),
val concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val lockedAt:
DateTime) : [DatabaseTaskLock](../-database-task-lock/index.md)

## Constructors

|                                    |                                                                                                                                                                                                                            |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [JdbcTaskLock](-jdbc-task-lock.md) | [jvm]<br>constructor(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), lockedAt: DateTime) |

## Properties

| Name                                     | Summary                                                                                                                                       |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrencyIndex](concurrency-index.md) | [jvm]<br>open override val [concurrencyIndex](concurrency-index.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) |
| [lockedAt](locked-at.md)                 | [jvm]<br>open override val [lockedAt](locked-at.md): DateTime                                                                                 |
| [name](name.md)                          | [jvm]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                    |

## Functions

| Name                     | Summary                                                                                                                               |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |

