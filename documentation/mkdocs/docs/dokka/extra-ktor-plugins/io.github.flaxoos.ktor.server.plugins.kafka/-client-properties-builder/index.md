---
title: ClientPropertiesBuilder
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[ClientPropertiesBuilder](index.md)



# ClientPropertiesBuilder

sealed class [ClientPropertiesBuilder](index.md) : [KafkaPropertiesBuilder](../-kafka-properties-builder/index.md)

see CommonClientConfigs



#### Inheritors


| |
|---|
| [CommonClientPropertiesBuilder](../-common-client-properties-builder/index.md) |
| [AdminPropertiesBuilder](../-admin-properties-builder/index.md) |
| [ProducerPropertiesBuilder](../-producer-properties-builder/index.md) |
| [ConsumerPropertiesBuilder](../-consumer-properties-builder/index.md) |


## Properties


| Name | Summary |
|---|---|
| [bootstrapServers](bootstrap-servers.md) | [jvm]<br>var [bootstrapServers](bootstrap-servers.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientDnsLookup](client-dns-lookup.md) | [jvm]<br>var [clientDnsLookup](client-dns-lookup.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientId](client-id.md) | [jvm]<br>var [clientId](client-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [clientRack](client-rack.md) | [jvm]<br>var [clientRack](client-rack.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [connectionsMaxIdleMs](connections-max-idle-ms.md) | [jvm]<br>var [connectionsMaxIdleMs](connections-max-idle-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metadataMaxAge](metadata-max-age.md) | [jvm]<br>var [metadataMaxAge](metadata-max-age.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricReporterClasses](metric-reporter-classes.md) | [jvm]<br>var [metricReporterClasses](metric-reporter-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsNumSamples](metrics-num-samples.md) | [jvm]<br>var [metricsNumSamples](metrics-num-samples.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsRecordingLevel](metrics-recording-level.md) | [jvm]<br>var [metricsRecordingLevel](metrics-recording-level.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [metricsSampleWindowMs](metrics-sample-window-ms.md) | [jvm]<br>var [metricsSampleWindowMs](metrics-sample-window-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [receiveBuffer](receive-buffer.md) | [jvm]<br>var [receiveBuffer](receive-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMaxMs](reconnect-backoff-max-ms.md) | [jvm]<br>var [reconnectBackoffMaxMs](reconnect-backoff-max-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMs](reconnect-backoff-ms.md) | [jvm]<br>var [reconnectBackoffMs](reconnect-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [requestTimeoutMs](request-timeout-ms.md) | [jvm]<br>var [requestTimeoutMs](request-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [retries](retries.md) | [jvm]<br>var [retries](retries.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [retryBackoffMs](retry-backoff-ms.md) | [jvm]<br>var [retryBackoffMs](retry-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [securityProtocol](security-protocol.md) | [jvm]<br>var [securityProtocol](security-protocol.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [sendBuffer](send-buffer.md) | [jvm]<br>var [sendBuffer](send-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |


## Functions


| Name | Summary |
|---|---|
| [build](build.md) | [jvm]<br>open override fun [build](build.md)(): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.md)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?&gt; |

