---
title: registerSchema
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](../index.md)/[SchemaRegistryClient](index.md)/[registerSchema](register-schema.md)

# registerSchema

[jvm]\
inline fun &lt;[T](register-schema.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)
&gt; [registerSchema](register-schema.md)(
klass: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)
&lt;out [T](register-schema.md)&gt;,
topicName: [TopicName](../../io.github.flaxoos.ktor.server.plugins.kafka/-topic-name/index.md), noinline onConflict: ()
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = {})

Register a schema to the schema registry using ktor client

#### Parameters

jvm

|            |                                                                                   |
|------------|-----------------------------------------------------------------------------------|
| klass      | the class to register, must be annotated with Serializable                        |
| topicName  | the topic name to associate the schema with                                       |
| onConflict | the function to run if a schema with the same name already exists, defaults to do |




