---
title: SchemaRegistryClient
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](../index.md)/[SchemaRegistryClient](index.md)



# SchemaRegistryClient



[jvm]\
class [SchemaRegistryClient](index.md)(providedClient: HttpClient, schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), timeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md))



## Constructors


| | |
|---|---|
| [SchemaRegistryClient](-schema-registry-client.md) | [jvm]<br>constructor(providedClient: HttpClient, schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), timeoutMs: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) |


## Properties


| Name | Summary |
|---|---|
| [client](client.md) | [jvm]<br>val [client](client.md): HttpClient |


## Functions


| Name | Summary |
|---|---|
| [registerSchema](register-schema.md) | [jvm]<br>inline fun &lt;[T](register-schema.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)&gt; [registerSchema](register-schema.md)(klass: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [T](register-schema.md)&gt;, topicName: [TopicName](../../io.github.flaxoos.ktor.server.plugins.kafka/-topic-name/index.md), noinline onConflict: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = {})<br>Register a schema to the schema registry using ktor client |
| [registerSchemas](register-schemas.md) | [jvm]<br>inline fun &lt;[T](register-schemas.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)&gt; [registerSchemas](register-schemas.md)(schemas: [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.md)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [T](register-schemas.md)&gt;, [TopicName](../../io.github.flaxoos.ktor.server.plugins.kafka/-topic-name/index.md)&gt;) |

