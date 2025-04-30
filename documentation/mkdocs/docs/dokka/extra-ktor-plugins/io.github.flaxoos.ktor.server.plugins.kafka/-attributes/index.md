---
title: Attributes
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[Attributes](index.md)

# Attributes

[jvm]\
object [Attributes](index.md)

## Properties

| Name                                                      | Summary                                                                                                                                                                                                                                                                                                          |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [AdminClientAttributeKey](-admin-client-attribute-key.md) | [jvm]<br>val [AdminClientAttributeKey](-admin-client-attribute-key.md): AttributeKey&lt;AdminClient&gt;<br>Attribute key for AdminClient                                                                                                                                                                         |
| [ConsumerAttributeKey](-consumer-attribute-key.md)        | [jvm]<br>val [ConsumerAttributeKey](-consumer-attribute-key.md): AttributeKey&lt;KafkaConsumer&lt;[KafkaRecordKey](../-kafka-record-key/index.md), GenericRecord&gt;&gt;<br>Attribute key for [Consumer](../-consumer/index.md)                                                                                  |
| [ProducerAttributeKey](-producer-attribute-key.md)        | [jvm]<br>val [ProducerAttributeKey](-producer-attribute-key.md): AttributeKey&lt;KafkaProducer&lt;[KafkaRecordKey](../-kafka-record-key/index.md), GenericRecord&gt;&gt;<br>Attribute key for [Producer](../-producer/index.md)                                                                                  |
| [SchemaRegistryClientKey](-schema-registry-client-key.md) | [jvm]<br>val [SchemaRegistryClientKey](-schema-registry-client-key.md): AttributeKey&lt;[SchemaRegistryClient](../../io.github.flaxoos.ktor.server.plugins.kafka.components/-schema-registry-client/index.md)&gt;<br>Attribute key for HttpClient used for registering Schemas to the configured schema registry |

