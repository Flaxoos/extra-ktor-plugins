---
title: KafkaConfig
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[KafkaConfig](index.md)



# KafkaConfig



[jvm]\
class [KafkaConfig](index.md) : [AbstractKafkaConfig](../-abstract-kafka-config/index.md)



## Constructors


| | |
|---|---|
| [KafkaConfig](-kafka-config.md) | [jvm]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [consumerConfig](../-abstract-kafka-config/consumer-config.md) | [jvm]<br>var [consumerConfig](../-abstract-kafka-config/consumer-config.md): [KafkaConsumerConfig](../-kafka-consumer-config/index.md)?<br>Because the consumer is operating in the background, it can be defined in the setup phase |
| [schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md) | [jvm]<br>var [schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>Schema registration timeout |
| [schemaRegistryUrl](schema-registry-url.md) | [jvm]<br>open override var [schemaRegistryUrl](schema-registry-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The schema registry url, if set, a client will be created and can be accessed later to register schemas manually, if schemas is left empty |


## Functions


| Name | Summary |
|---|---|
| [admin](../admin.md) | [jvm]<br>fun [KafkaConfig](index.md).[admin](../admin.md)(configuration: [AdminPropertiesBuilder](../-admin-properties-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { AdminPropertiesBuilder() }) |
| [common](../common.md) | [jvm]<br>fun [KafkaConfig](index.md).[common](../common.md)(configuration: [CommonClientPropertiesBuilder](../-common-client-properties-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { CommonClientPropertiesBuilder }) |
| [consumer](../consumer.md) | [jvm]<br>fun [KafkaConfig](index.md).[consumer](../consumer.md)(configuration: [ConsumerPropertiesBuilder](../-consumer-properties-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { ConsumerPropertiesBuilder(schemaRegistryUrl) }) |
| [consumerConfig](../consumer-config.md) | [jvm]<br>fun [AbstractKafkaConfig](../-abstract-kafka-config/index.md).[consumerConfig](../consumer-config.md)(configuration: [KafkaConsumerConfig](../-kafka-consumer-config/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { }) |
| [producer](../producer.md) | [jvm]<br>fun [KafkaConfig](index.md).[producer](../producer.md)(configuration: [ProducerPropertiesBuilder](../-producer-properties-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { ProducerPropertiesBuilder(schemaRegistryUrl) }) |
| [registerSchemas](../register-schemas.md) | [jvm]<br>fun [AbstractKafkaConfig](../-abstract-kafka-config/index.md).[registerSchemas](../register-schemas.md)(configuration: [SchemaRegistrationBuilder](../-schema-registration-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { SchemaRegistrationBuilder() }) |
| [topic](../topic.md) | [jvm]<br>fun [KafkaConfig](index.md).[topic](../topic.md)(name: [TopicName](../-topic-name/index.md), block: [TopicBuilder](../-topic-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)) |

