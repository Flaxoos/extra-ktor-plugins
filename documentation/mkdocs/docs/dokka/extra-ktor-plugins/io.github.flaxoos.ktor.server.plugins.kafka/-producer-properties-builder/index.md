---
title: ProducerPropertiesBuilder
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[ProducerPropertiesBuilder](index.md)

# ProducerPropertiesBuilder

[jvm]\
class [ProducerPropertiesBuilder](index.md)(var
schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? =
null) : [ClientPropertiesBuilder](../-client-properties-builder/index.md), SchemaRegistryProvider

see ProducerConfig

## Constructors

|                                                              |                                                                                                                                 |
|--------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| [ProducerPropertiesBuilder](-producer-properties-builder.md) | [jvm]<br>constructor(schemaRegistryUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)? = null) |

## Properties

| Name                                                                               | Summary                                                                                                                                                                    |
|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [acks](acks.md)                                                                    | [jvm]<br>var [acks](acks.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                                                    |
| [batchSize](batch-size.md)                                                         | [jvm]<br>var [batchSize](batch-size.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                                         |
| [bootstrapServers](../-client-properties-builder/bootstrap-servers.md)             | [jvm]<br>var [bootstrapServers](../-client-properties-builder/bootstrap-servers.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?             |
| [bufferMemory](buffer-memory.md)                                                   | [jvm]<br>var [bufferMemory](buffer-memory.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                                   |
| [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md)              | [jvm]<br>var [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?              |
| [clientId](../-client-properties-builder/client-id.md)                             | [jvm]<br>var [clientId](../-client-properties-builder/client-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                             |
| [clientRack](../-client-properties-builder/client-rack.md)                         | [jvm]<br>var [clientRack](../-client-properties-builder/client-rack.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                         |
| [compressionType](compression-type.md)                                             | [jvm]<br>var [compressionType](compression-type.md): [CompressionType](../-compression-type/index.md)?                                                                     |
| [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md)   | [jvm]<br>var [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?   |
| [deliveryTimeoutMs](delivery-timeout-ms.md)                                        | [jvm]<br>var [deliveryTimeoutMs](delivery-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                        |
| [enableIdempotence](enable-idempotence.md)                                         | [jvm]<br>var [enableIdempotence](enable-idempotence.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                         |
| [interceptorClasses](interceptor-classes.md)                                       | [jvm]<br>var [interceptorClasses](interceptor-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                       |
| [keySerializerClass](key-serializer-class.md)                                      | [jvm]<br>var [keySerializerClass](key-serializer-class.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                      |
| [lingerMs](linger-ms.md)                                                           | [jvm]<br>var [lingerMs](linger-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                                           |
| [maxBlockMs](max-block-ms.md)                                                      | [jvm]<br>var [maxBlockMs](max-block-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                                      |
| [maxInFlightRequestsPerConnection](max-in-flight-requests-per-connection.md)       | [jvm]<br>var [maxInFlightRequestsPerConnection](max-in-flight-requests-per-connection.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?       |
| [maxRequestSize](max-request-size.md)                                              | [jvm]<br>var [maxRequestSize](max-request-size.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                              |
| [metadataMaxAge](../-client-properties-builder/metadata-max-age.md)                | [jvm]<br>var [metadataMaxAge](../-client-properties-builder/metadata-max-age.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                |
| [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md)  | [jvm]<br>var [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?  |
| [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md)          | [jvm]<br>var [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?          |
| [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md)  | [jvm]<br>var [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?  |
| [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md) | [jvm]<br>var [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [partitionerClass](partitioner-class.md)                                           | [jvm]<br>var [partitionerClass](partitioner-class.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                           |
| [receiveBuffer](../-client-properties-builder/receive-buffer.md)                   | [jvm]<br>var [receiveBuffer](../-client-properties-builder/receive-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                   |
| [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md) | [jvm]<br>var [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md)        | [jvm]<br>var [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?        |
| [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md)            | [jvm]<br>var [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?            |
| [retries](../-client-properties-builder/retries.md)                                | [jvm]<br>var [retries](../-client-properties-builder/retries.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                |
| [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md)                | [jvm]<br>var [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                |
| [schemaRegistryUrl](schema-registry-url.md)                                        | [jvm]<br>open override var [schemaRegistryUrl](schema-registry-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)?                    |
| [securityProtocol](../-client-properties-builder/security-protocol.md)             | [jvm]<br>var [securityProtocol](../-client-properties-builder/security-protocol.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?             |
| [sendBuffer](../-client-properties-builder/send-buffer.md)                         | [jvm]<br>var [sendBuffer](../-client-properties-builder/send-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                         |
| [transactionalId](transactional-id.md)                                             | [jvm]<br>var [transactionalId](transactional-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                             |
| [transactionTimeout](transaction-timeout.md)                                       | [jvm]<br>var [transactionTimeout](transaction-timeout.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                       |
| [valueSerializerClass](value-serializer-class.md)                                  | [jvm]<br>var [valueSerializerClass](value-serializer-class.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                  |

## Functions

| Name              | Summary                                                                                          |
|-------------------|--------------------------------------------------------------------------------------------------|
| [build](build.md) | [jvm]<br>open override fun [build](build.md)(): [KafkaProperties](../-kafka-properties/index.md) |

