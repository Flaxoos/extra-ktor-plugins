---
title: SchemaRegistrationBuilder
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[SchemaRegistrationBuilder](index.md)

# SchemaRegistrationBuilder

[jvm]\
class [SchemaRegistrationBuilder](index.md)

## Constructors

|                                                              |                        |
|--------------------------------------------------------------|------------------------|
| [SchemaRegistrationBuilder](-schema-registration-builder.md) | [jvm]<br>constructor() |

## Functions

| Name              | Summary                                                                                                                                                                                                                                                                                                                                                      |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [at](at.md)       | [jvm]<br>infix fun [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)&gt;.[at](at.md)(topicName: [TopicName](../-topic-name/index.md))                                                                                                  |
| [using](using.md) | [jvm]<br>fun [using](using.md)(provider: () -&gt; HttpClient)<br>optionally provide a client to register schemas, by default, CIO would be used. In any case, the following it would be configured with serialization json and the configured [AbstractKafkaConfig.schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md) |

