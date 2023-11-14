---
title: TopicPropertiesBuilder
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[TopicPropertiesBuilder](index.md)



# TopicPropertiesBuilder



[jvm]\
class [TopicPropertiesBuilder](index.md) : [KafkaPropertiesBuilder](../-kafka-properties-builder/index.md)

See TopicConfig



## Constructors


| | |
|---|---|
| [TopicPropertiesBuilder](-topic-properties-builder.md) | [jvm]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [cleanupPolicy](cleanup-policy.md) | [jvm]<br>var [cleanupPolicy](cleanup-policy.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? |
| [compressionType](compression-type.md) | [jvm]<br>var [compressionType](compression-type.md): [CompressionType](../-compression-type/index.md)? |
| [deleteRetentionMs](delete-retention-ms.md) | [jvm]<br>var [deleteRetentionMs](delete-retention-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [fileDeleteDelayMs](file-delete-delay-ms.md) | [jvm]<br>var [fileDeleteDelayMs](file-delete-delay-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [flushMessagesInterval](flush-messages-interval.md) | [jvm]<br>var [flushMessagesInterval](flush-messages-interval.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [flushMs](flush-ms.md) | [jvm]<br>var [flushMs](flush-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [indexIntervalBytes](index-interval-bytes.md) | [jvm]<br>var [indexIntervalBytes](index-interval-bytes.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)? |
| [maxCompactionLagMs](max-compaction-lag-ms.md) | [jvm]<br>var [maxCompactionLagMs](max-compaction-lag-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [maxMessageBytes](max-message-bytes.md) | [jvm]<br>var [maxMessageBytes](max-message-bytes.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)? |
| [messageDownconversionEnable](message-downconversion-enable.md) | [jvm]<br>var [messageDownconversionEnable](message-downconversion-enable.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)? |
| [messageFormatVersion](message-format-version.md) | [jvm]<br>var [messageFormatVersion](message-format-version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? |
| [messageTimestampDifferenceMaxMs](message-timestamp-difference-max-ms.md) | [jvm]<br>var [messageTimestampDifferenceMaxMs](message-timestamp-difference-max-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [messageTimestampType](message-timestamp-type.md) | [jvm]<br>var [messageTimestampType](message-timestamp-type.md): [MessageTimestampType](../-message-timestamp-type/index.md)? |
| [minCleanableDirtyRatio](min-cleanable-dirty-ratio.md) | [jvm]<br>var [minCleanableDirtyRatio](min-cleanable-dirty-ratio.md): [Float](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.md)? |
| [minCompactionLagMs](min-compaction-lag-ms.md) | [jvm]<br>var [minCompactionLagMs](min-compaction-lag-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [minInSyncReplicas](min-in-sync-replicas.md) | [jvm]<br>var [minInSyncReplicas](min-in-sync-replicas.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)? |
| [preallocate](preallocate.md) | [jvm]<br>var [preallocate](preallocate.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)? |
| [retentionBytes](retention-bytes.md) | [jvm]<br>var [retentionBytes](retention-bytes.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [retentionMs](retention-ms.md) | [jvm]<br>var [retentionMs](retention-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [segmentBytes](segment-bytes.md) | [jvm]<br>var [segmentBytes](segment-bytes.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)? |
| [segmentIndexBytes](segment-index-bytes.md) | [jvm]<br>var [segmentIndexBytes](segment-index-bytes.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)? |
| [segmentJitterMs](segment-jitter-ms.md) | [jvm]<br>var [segmentJitterMs](segment-jitter-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [segmentMs](segment-ms.md) | [jvm]<br>var [segmentMs](segment-ms.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)? |
| [uncleanLeaderElectionEnable](unclean-leader-election-enable.md) | [jvm]<br>var [uncleanLeaderElectionEnable](unclean-leader-election-enable.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)? |


## Functions


| Name | Summary |
|---|---|
| [build](build.md) | [jvm]<br>open override fun [build](build.md)(): [KafkaProperties](../-kafka-properties/index.md) |

