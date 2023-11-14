---
title: subscribe
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](index.md)/[subscribe](subscribe.md)



# subscribe



[jvm]\
fun Application.[subscribe](subscribe.md)(consumer: [Consumer](../io.github.flaxoos.ktor.server.plugins.kafka/-consumer/index.md), pollFrequency: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), topics: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md)&lt;[TopicName](../io.github.flaxoos.ktor.server.plugins.kafka/-topic-name/index.md)&gt;): Flow&lt;ConsumerRecord&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), GenericRecord&gt;&gt;



Subscribes a [Consumer](../io.github.flaxoos.ktor.server.plugins.kafka/-consumer/index.md) to a list of topics, returning a flow of records



#### Receiver



Application the ktor server application



#### Return



Flow of records



#### Parameters


jvm

| | |
|---|---|
| consumer | [Consumer](../io.github.flaxoos.ktor.server.plugins.kafka/-consumer/index.md) to subscribe |
| pollFrequency | [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) at what frequency should the consumer poll, in practice the timeout passed to KafkaConsumer.poll |
| topics | [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.md) of topics to subscribe to |




