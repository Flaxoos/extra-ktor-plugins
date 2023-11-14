---
title: Kafka
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](index.md)/[Kafka](-kafka.md)



# Kafka



[jvm]\
val [Kafka](-kafka.md): ApplicationPlugin&lt;[KafkaConfig](-kafka-config/index.md)&gt;



Plugin for setting up a kafka client



Example:

```kotlin
install(Kafka) {
     schemaRegistryUrl = listOf(super.schemaRegistryUrl)
     topic(it) {
         partitions = 1
         replicas = 1
         configs {
             messageTimestampType = CreateTime
         }
     }
     common { bootstrapServers = listOf("my-kafka") }
     admin { } // will create an admin
     producer { clientId = "my-client-id" } // will create a producer
     consumer { groupId = "my-group-id" } // will create a consumer
     consumerConfig {
         consumerRecordHandler("my-topic) { record ->
             myService.save(record)
         )
    }
}
```



