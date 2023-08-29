package io.flax.ktor.server.plugins.components

import io.flax.ktor.server.plugins.KafkaRecordKey
import io.flax.ktor.server.plugins.Producer
import io.flax.ktor.server.plugins.ProducerPropertiesBuilder
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer

internal fun Map<String, Any?>.createProducer(): Producer = KafkaProducer<KafkaRecordKey, GenericRecord>(this)
