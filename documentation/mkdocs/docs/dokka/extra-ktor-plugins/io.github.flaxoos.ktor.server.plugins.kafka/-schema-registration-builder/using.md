---
title: using
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.kafka](../index.md)/[SchemaRegistrationBuilder](index.md)/[using](using.md)



# using



[jvm]\
fun [using](using.md)(provider: () -&gt; HttpClient)



optionally provide a client to register schemas, by default, CIO would be used. In any case, the following it would be configured with serialization json and the configured [AbstractKafkaConfig.schemaRegistrationTimeoutMs](../-abstract-kafka-config/schema-registration-timeout-ms.md)




