---
title: fromRecord
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka.components](index.md)/[fromRecord](from-record.md)



# fromRecord



[jvm]\
inline fun &lt;[T](from-record.md)&gt; [fromRecord](from-record.md)(record: GenericRecord): [T](from-record.md)



converts a GenericRecord to a [T](from-record.md)



#### Return



the resulting [T](from-record.md) must be annotated with Serializable



#### Parameters


jvm

| | |
|---|---|
| record | GenericRecord |



#### Throws


| | |
|---|---|
| SerializationException | if serialization fails |



