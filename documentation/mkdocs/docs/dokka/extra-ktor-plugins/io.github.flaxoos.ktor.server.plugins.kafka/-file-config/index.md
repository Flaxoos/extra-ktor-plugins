---
title: FileConfig
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[FileConfig](index.md)

# FileConfig

[jvm]\
object [FileConfig](index.md)

## Properties

| Name               | Summary                                                                                                                                                                                             |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Kafka](-kafka.md) | [jvm]<br>val [Kafka](-kafka.md): ApplicationPlugin&lt;[KafkaFileConfig](../-kafka-file-config/index.md)&gt;<br>Plugin for setting up a kafka client, configured in application config file Example: |

## Functions

| Name               | Summary                                                                                                                                                                                                                                                                                                                                                                                                                             |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Kafka](-kafka.md) | [jvm]<br>fun [Kafka](-kafka.md)(configurationPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)): ApplicationPlugin&lt;[KafkaFileConfig](../-kafka-file-config/index.md)&gt;                                                                                                                                                                                                                      |
| [kafka](kafka.md)  | [jvm]<br>fun Application.[kafka](kafka.md)(configurationPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) = DEFAULT_CONFIG_PATH, config: [KafkaFileConfig](../-kafka-file-config/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Installs the [Kafka](-kafka.md) plugin with the given [KafkaFileConfig](../-kafka-file-config/index.md) block |

