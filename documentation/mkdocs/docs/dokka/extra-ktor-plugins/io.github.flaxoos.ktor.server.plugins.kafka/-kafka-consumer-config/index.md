---
title: KafkaConsumerConfig
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[KafkaConsumerConfig](index.md)



# KafkaConsumerConfig



[jvm]\
class [KafkaConsumerConfig](index.md)



## Constructors


| | |
|---|---|
| [KafkaConsumerConfig](-kafka-consumer-config.md) | [jvm]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [consumerPollFrequency](consumer-poll-frequency.md) | [jvm]<br>val [consumerPollFrequency](consumer-poll-frequency.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) |
| [consumerRecordHandlers](consumer-record-handlers.md) | [jvm]<br>val [consumerRecordHandlers](consumer-record-handlers.md): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.md)&lt;[TopicName](../-topic-name/index.md), [ConsumerRecordHandler](../-consumer-record-handler/index.md)&gt; |


## Functions


| Name | Summary |
|---|---|
| [consumerRecordHandler](../consumer-record-handler.md) | [jvm]<br>fun [KafkaConsumerConfig](index.md).[consumerRecordHandler](../consumer-record-handler.md)(topicName: [TopicName](../-topic-name/index.md), handler: [ConsumerRecordHandler](../-consumer-record-handler/index.md)) |

