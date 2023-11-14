---
title: KafkaFileConfig
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[KafkaFileConfig](index.md)



# KafkaFileConfig



[jvm]\
class [KafkaFileConfig](index.md)(config: ApplicationConfig) : [AbstractKafkaConfig](../-abstract-kafka-config/index.md)

Configuration for the Kafka plugin



## Constructors


| | |
|---|---|
| [KafkaFileConfig](-kafka-file-config.md) | [jvm]<br>constructor(config: ApplicationConfig) |


## Properties


| Name | Summary |
|---|---|
| [consumerConfig](../-abstract-kafka-config/consumer-config.md) | [jvm]<br>var [consumerConfig](../-abstract-kafka-config/consumer-config.md): [KafkaConsumerConfig](../-kafka-consumer-config/index.md)?<br>Because the consumer is operating in the background, it can be defined in the setup phase |
| [schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md) | [jvm]<br>var [schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>Schema registration timeout |
| [schemaRegistryUrl](schema-registry-url.md) | [jvm]<br>open override var [schemaRegistryUrl](schema-registry-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The schema registry url, if set, a client will be created and can be accessed later to register schemas manually, if schemas is left empty |


## Functions


| Name | Summary |
|---|---|
| [consumerConfig](../consumer-config.md) | [jvm]<br>fun [AbstractKafkaConfig](../-abstract-kafka-config/index.md).[consumerConfig](../consumer-config.md)(configuration: [KafkaConsumerConfig](../-kafka-consumer-config/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { }) |
| [registerSchemas](../register-schemas.md) | [jvm]<br>fun [AbstractKafkaConfig](../-abstract-kafka-config/index.md).[registerSchemas](../register-schemas.md)(configuration: [SchemaRegistrationBuilder](../-schema-registration-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { SchemaRegistrationBuilder() }) |

