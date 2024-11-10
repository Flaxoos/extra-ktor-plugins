---
title: ConsumerRecordHandler
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[ConsumerRecordHandler](index.md)

# ConsumerRecordHandler

[jvm]\
typealias [ConsumerRecordHandler](index.md) = suspend Application.(
ConsumerRecord&lt;[KafkaRecordKey](../-kafka-record-key/index.md), GenericRecord&gt;)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)


