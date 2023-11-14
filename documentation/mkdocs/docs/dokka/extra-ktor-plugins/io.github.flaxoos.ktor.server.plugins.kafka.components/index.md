---
title: io.github.flaxoos.ktor.server.plugins.kafka.components
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](index.md)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [SchemaRegistryClient](-schema-registry-client/index.md) | [jvm]<br>class [SchemaRegistryClient](-schema-registry-client/index.md)(providedClient: HttpClient, schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), timeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) |


## Functions


| Name | Summary |
|---|---|
| [createSchemaRegistryClient](create-schema-registry-client.md) | [jvm]<br>fun [createSchemaRegistryClient](create-schema-registry-client.md)(schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), timeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md), clientProvider: () -&gt; HttpClient): [SchemaRegistryClient](-schema-registry-client/index.md) |
| [fromRecord](from-record.md) | [jvm]<br>inline fun &lt;[T](from-record.md)&gt; [fromRecord](from-record.md)(record: GenericRecord): [T](from-record.md)<br>converts a GenericRecord to a [T](from-record.md) |
| [subscribe](subscribe.md) | [jvm]<br>fun Application.[subscribe](subscribe.md)(consumer: [Consumer](../io.github.flaxoos.ktor.server.plugins.kafka/-consumer/index.md), pollFrequency: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), topics: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[TopicName](../io.github.flaxoos.ktor.server.plugins.kafka/-topic-name/index.md)&gt;): Flow&lt;ConsumerRecord&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), GenericRecord&gt;&gt;<br>Subscribes a [Consumer](../io.github.flaxoos.ktor.server.plugins.kafka/-consumer/index.md) to a list of topics, returning a flow of records |
| [toRecord](to-record.md) | [jvm]<br>inline fun &lt;[T](to-record.md)&gt; [T](to-record.md).[toRecord](to-record.md)(): GenericRecord<br>converts a [T](to-record.md) to a GenericRecord |

