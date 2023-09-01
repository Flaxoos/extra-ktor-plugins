package io.github.flaxoos.ktor.server.plugins.kafka.components

import io.github.flaxoos.ktor.server.plugins.kafka.KafkaRecordKey
import io.github.flaxoos.ktor.server.plugins.kafka.Producer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer

internal fun Map<String, Any?>.createProducer(): Producer = KafkaProducer<KafkaRecordKey, GenericRecord>(this)
