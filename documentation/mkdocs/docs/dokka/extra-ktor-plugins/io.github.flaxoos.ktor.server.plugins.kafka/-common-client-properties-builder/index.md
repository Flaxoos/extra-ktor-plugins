---
title: CommonClientPropertiesBuilder
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[CommonClientPropertiesBuilder](index.md)

# CommonClientPropertiesBuilder

[jvm]\
data
object [CommonClientPropertiesBuilder](index.md) : [ClientPropertiesBuilder](../-client-properties-builder/index.md)

Concrete implementation of [ClientPropertiesBuilder](../-client-properties-builder/index.md) to represent the common
properties

## Properties

| Name                                                                               | Summary                                                                                                                                                                    |
|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [bootstrapServers](../-client-properties-builder/bootstrap-servers.md)             | [jvm]<br>var [bootstrapServers](../-client-properties-builder/bootstrap-servers.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?             |
| [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md)              | [jvm]<br>var [clientDnsLookup](../-client-properties-builder/client-dns-lookup.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?              |
| [clientId](../-client-properties-builder/client-id.md)                             | [jvm]<br>var [clientId](../-client-properties-builder/client-id.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                             |
| [clientRack](../-client-properties-builder/client-rack.md)                         | [jvm]<br>var [clientRack](../-client-properties-builder/client-rack.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                         |
| [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md)   | [jvm]<br>var [connectionsMaxIdleMs](../-client-properties-builder/connections-max-idle-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?   |
| [metadataMaxAge](../-client-properties-builder/metadata-max-age.md)                | [jvm]<br>var [metadataMaxAge](../-client-properties-builder/metadata-max-age.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                |
| [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md)  | [jvm]<br>var [metricReporterClasses](../-client-properties-builder/metric-reporter-classes.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?  |
| [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md)          | [jvm]<br>var [metricsNumSamples](../-client-properties-builder/metrics-num-samples.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?          |
| [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md)  | [jvm]<br>var [metricsRecordingLevel](../-client-properties-builder/metrics-recording-level.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?  |
| [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md) | [jvm]<br>var [metricsSampleWindowMs](../-client-properties-builder/metrics-sample-window-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [receiveBuffer](../-client-properties-builder/receive-buffer.md)                   | [jvm]<br>var [receiveBuffer](../-client-properties-builder/receive-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                   |
| [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md) | [jvm]<br>var [reconnectBackoffMaxMs](../-client-properties-builder/reconnect-backoff-max-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)? |
| [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md)        | [jvm]<br>var [reconnectBackoffMs](../-client-properties-builder/reconnect-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?        |
| [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md)            | [jvm]<br>var [requestTimeoutMs](../-client-properties-builder/request-timeout-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?            |
| [retries](../-client-properties-builder/retries.md)                                | [jvm]<br>var [retries](../-client-properties-builder/retries.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                                |
| [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md)                | [jvm]<br>var [retryBackoffMs](../-client-properties-builder/retry-backoff-ms.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                |
| [securityProtocol](../-client-properties-builder/security-protocol.md)             | [jvm]<br>var [securityProtocol](../-client-properties-builder/security-protocol.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?             |
| [sendBuffer](../-client-properties-builder/send-buffer.md)                         | [jvm]<br>var [sendBuffer](../-client-properties-builder/send-buffer.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?                         |

## Functions

| Name                                            | Summary                                                                                                                                                                                                                                                                                                                                            |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [build](../-client-properties-builder/build.md) | [jvm]<br>open override fun [build](../-client-properties-builder/build.md)(): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.md)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.md)?&gt; |

