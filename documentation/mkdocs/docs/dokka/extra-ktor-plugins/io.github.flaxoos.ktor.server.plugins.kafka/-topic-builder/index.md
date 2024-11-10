---
title: TopicBuilder
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[TopicBuilder](index.md)

# TopicBuilder

[jvm]\
class [TopicBuilder](index.md)(name: [TopicName](../-topic-name/index.md))

## Constructors

|                                   |                                                                  |
|-----------------------------------|------------------------------------------------------------------|
| [TopicBuilder](-topic-builder.md) | [jvm]<br>constructor(name: [TopicName](../-topic-name/index.md)) |

## Types

| Name                             | Summary                                          |
|----------------------------------|--------------------------------------------------|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name                                           | Summary                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [partitions](partitions.md)                    | [jvm]<br>var [partitions](partitions.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)                                                                                                                                                                                                                                                                                          |
| [replicas](replicas.md)                        | [jvm]<br>var [replicas](replicas.md): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-short/index.md)                                                                                                                                                                                                                                                                                          |
| [replicasAssignments](replicas-assignments.md) | [jvm]<br>var [replicasAssignments](replicas-assignments.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.md)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)?&gt;&gt;? |

## Functions

| Name                  | Summary                                                                                                                                                                                         |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [configs](configs.md) | [jvm]<br>fun [configs](configs.md)(config: [TopicPropertiesBuilder](../-topic-properties-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)) |

