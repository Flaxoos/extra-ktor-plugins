---
title: ConsumerPropertiesBuilder
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[ConsumerPropertiesBuilder](index.md)



# ConsumerPropertiesBuilder



[jvm]\
class [ConsumerPropertiesBuilder](index.md)(var schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null) : [ClientPropertiesBuilder](../-client-properties-builder/index.md), SchemaRegistryProvider

see ConsumerConfig



## Constructors


| | |
|---|---|
| [ConsumerPropertiesBuilder](-consumer-properties-builder.md) | [jvm]<br>constructor(schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null) |


## Properties


| Name | Summary |
|---|---|
| [allowAutoCreateTopics](allow-auto-create-topics.md) | [jvm]<br>var [allowAutoCreateTopics](allow-auto-create-topics.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [autoCommitIntervalMs](auto-commit-interval-ms.md) | [jvm]<br>var [autoCommitIntervalMs](auto-commit-interval-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [autoOffsetReset](auto-offset-reset.md) | [jvm]<br>var [autoOffsetReset](auto-offset-reset.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [bootstrapServers](../-client-properties-builder/bootstrap-servers.md) | [jvm]<br>var [bootstrapServers](../-client-properties-builder/bootstrap-servers.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [checkCrcs](check-crcs.md) | [jvm]<br>var [checkCrcs](check-crcs.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md) | [jvm]<br>var [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientId](../-client-properties-builder/client-id.md) | [jvm]<br>var [clientId](../-client-properties-builder/client-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientRack](../-client-properties-builder/client-rack.md) | [jvm]<br>var [clientRack](../-client-properties-builder/client-rack.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md) | [jvm]<br>var [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [defaultApiTimeoutMs](default-api-timeout-ms.md) | [jvm]<br>var [defaultApiTimeoutMs](default-api-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [enableAutoCommit](enable-auto-commit.md) | [jvm]<br>var [enableAutoCommit](enable-auto-commit.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [excludeInternalTopics](exclude-internal-topics.md) | [jvm]<br>var [excludeInternalTopics](exclude-internal-topics.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [fetchMaxBytes](fetch-max-bytes.md) | [jvm]<br>var [fetchMaxBytes](fetch-max-bytes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [fetchMaxWaitMs](fetch-max-wait-ms.md) | [jvm]<br>var [fetchMaxWaitMs](fetch-max-wait-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [fetchMinBytes](fetch-min-bytes.md) | [jvm]<br>var [fetchMinBytes](fetch-min-bytes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [groupId](group-id.md) | [jvm]<br>var [groupId](group-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [groupInstanceId](group-instance-id.md) | [jvm]<br>var [groupInstanceId](group-instance-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [heartbeatIntervalMs](heartbeat-interval-ms.md) | [jvm]<br>var [heartbeatIntervalMs](heartbeat-interval-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [interceptorClasses](interceptor-classes.md) | [jvm]<br>var [interceptorClasses](interceptor-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [isolationLevel](isolation-level.md) | [jvm]<br>var [isolationLevel](isolation-level.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [keyDeserializerClass](key-deserializer-class.md) | [jvm]<br>var [keyDeserializerClass](key-deserializer-class.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [maxPartitionFetchBytes](max-partition-fetch-bytes.md) | [jvm]<br>var [maxPartitionFetchBytes](max-partition-fetch-bytes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [maxPollIntervalMs](max-poll-interval-ms.md) | [jvm]<br>var [maxPollIntervalMs](max-poll-interval-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [maxPollRecords](max-poll-records.md) | [jvm]<br>var [maxPollRecords](max-poll-records.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metadataMaxAge](../-client-properties-builder/metadata-max-age.md) | [jvm]<br>var [metadataMaxAge](../-client-properties-builder/metadata-max-age.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md) | [jvm]<br>var [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md) | [jvm]<br>var [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md) | [jvm]<br>var [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md) | [jvm]<br>var [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [partitionAssignmentStrategy](partition-assignment-strategy.md) | [jvm]<br>var [partitionAssignmentStrategy](partition-assignment-strategy.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [receiveBuffer](../-client-properties-builder/receive-buffer.md) | [jvm]<br>var [receiveBuffer](../-client-properties-builder/receive-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md) | [jvm]<br>var [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md) | [jvm]<br>var [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md) | [jvm]<br>var [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [retries](../-client-properties-builder/retries.md) | [jvm]<br>var [retries](../-client-properties-builder/retries.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md) | [jvm]<br>var [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [schemaRegistryUrl](schema-registry-url.md) | [jvm]<br>open override var [schemaRegistryUrl](schema-registry-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? |
| [securityProtocol](../-client-properties-builder/security-protocol.md) | [jvm]<br>var [securityProtocol](../-client-properties-builder/security-protocol.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [sendBuffer](../-client-properties-builder/send-buffer.md) | [jvm]<br>var [sendBuffer](../-client-properties-builder/send-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [sessionTimeoutMs](session-timeout-ms.md) | [jvm]<br>var [sessionTimeoutMs](session-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [valueDeserializerClass](value-deserializer-class.md) | [jvm]<br>var [valueDeserializerClass](value-deserializer-class.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |


## Functions


| Name | Summary |
|---|---|
| [build](build.md) | [jvm]<br>open override fun [build](build.md)(): [KafkaProperties](../-kafka-properties/index.md) |

