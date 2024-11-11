---
title: AbstractKafkaConfig
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[AbstractKafkaConfig](index.md)

# AbstractKafkaConfig

sealed class [AbstractKafkaConfig](index.md)

#### Inheritors

|                                                   |
|---------------------------------------------------|
| [KafkaConfig](../-kafka-config/index.md)          |
| [KafkaFileConfig](../-kafka-file-config/index.md) |

## Properties

| Name                                                             | Summary                                                                                                                                                                                                                                                                                          |
|------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [consumerConfig](consumer-config.md)                             | [jvm]<br>var [consumerConfig](consumer-config.md): [KafkaConsumerConfig](../-kafka-consumer-config/index.md)?<br>Because the consumer is operating in the background, it can be defined in the setup phase                                                                                       |
| [schemaRegistrationTimeoutMs](schema-registration-timeout-ms.md) | [jvm]<br>var [schemaRegistrationTimeoutMs](schema-registration-timeout-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>Schema registration timeout                                                                                                         |
| [schemaRegistryUrl](schema-registry-url.md)                      | [jvm]<br>abstract val [schemaRegistryUrl](schema-registry-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?<br>The schema registry url, if set, a client will be created and can be accessed later to register schemas manually, if schemas is left empty |

## Functions

| Name                                      | Summary                                                                                                                                                                                                                                                                                            |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [consumerConfig](../consumer-config.md)   | [jvm]<br>fun [AbstractKafkaConfig](index.md).[consumerConfig](../consumer-config.md)(configuration: [KafkaConsumerConfig](../-kafka-consumer-config/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { })                                           |
| [registerSchemas](../register-schemas.md) | [jvm]<br>fun [AbstractKafkaConfig](index.md).[registerSchemas](../register-schemas.md)(configuration: [SchemaRegistrationBuilder](../-schema-registration-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md) = { SchemaRegistrationBuilder() }) |

