---
title: MongoDbTaskLock
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database](../index.md)/[MongoDbTaskLock](index.md)

# MongoDbTaskLock

[jvm]\
data class [MongoDbTaskLock](index.md)(val
name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), val
concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), var lockedAt:
DateTime) : [DatabaseTaskLock](../-database-task-lock/index.md)

## Constructors

|                                           |                                                                                                                                                                                                                            |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [MongoDbTaskLock](-mongo-db-task-lock.md) | [jvm]<br>constructor(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), concurrencyIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), lockedAt: DateTime) |

## Properties

| Name                                     | Summary                                                                                                                                       |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| [concurrencyIndex](concurrency-index.md) | [jvm]<br>open override val [concurrencyIndex](concurrency-index.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) |
| [lockedAt](locked-at.md)                 | [jvm]<br>open override var [lockedAt](locked-at.md): DateTime                                                                                 |
| [name](name.md)                          | [jvm]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                    |

## Functions

| Name                     | Summary                                                                                                                               |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |

