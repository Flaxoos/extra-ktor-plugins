---
title: toRecord
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](index.md)/[toRecord](to-record.md)



# toRecord



[jvm]\
inline fun &lt;[T](to-record.md)&gt; [T](to-record.md).[toRecord](to-record.md)(): GenericRecord



converts a [T](to-record.md) to a GenericRecord



#### Receiver



the [T](to-record.md) to convert, must be annotated with Serializable



#### Return



the resulting GenericRecord



#### Throws


| | |
|---|---|
| SerializationException | if serialization fails |



