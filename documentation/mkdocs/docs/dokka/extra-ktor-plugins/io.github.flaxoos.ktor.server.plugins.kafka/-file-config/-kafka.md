---
title: Kafka
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[FileConfig](index.md)/[Kafka](-kafka.md)

# Kafka

[jvm]\
fun [Kafka](-kafka.md)(
configurationPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)):
ApplicationPlugin&lt;[KafkaFileConfig](../-kafka-file-config/index.md)&gt;

[jvm]\
val [Kafka](-kafka.md): ApplicationPlugin&lt;[KafkaFileConfig](../-kafka-file-config/index.md)&gt;

Plugin for setting up a kafka client, configured in application config file Example:

```kotlin
install(Kafka) {
     consumerConfig {
         consumerRecordHandler("my-topic) { record ->
             myService.save(record)
         )
    }
}
@receiver [Application] the ktor server application
@param configurationPath The path to the configuration in the application configuration file
@param config Configuration block for the plugin, see [KafkaConsumerConfig]
```



