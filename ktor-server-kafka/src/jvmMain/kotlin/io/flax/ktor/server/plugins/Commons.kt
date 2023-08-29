package io.flax.ktor.server.plugins

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import kotlinx.coroutines.flow.Flow
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

@JvmInline
value class TopicName(val name: String) {
    companion object {
        infix fun named(name: String) = TopicName(name)
    }
}

const val KTOR_KAFKA_ADMIN_CLIENT_ATTRIBUTE_KEY = "KTOR_KAFKA_ADMIN_CLIENT"
const val KTOR_KAFKA_PRODUCER_ATTRIBUTE_KEY = "KTOR_KAFKA_PRODUCER"
const val KTOR_KAFKA_CONSUMER_ATTRIBUTE_KEY = "KTOR_KAFKA_CONSUMER"
const val KTOR_KAFKA_CONSUMER_WRAPPER_ATTRIBUTE_KEY = "KTOR_KAFKA_CONSUMER"

const val DEFAULT_CLIENT_ID = "ktor-producer"
const val DEFAULT_GROUP_ID = "ktor-consumer"
const val DEFAULT_CONFIG_PATH = "ktor.kafka"
const val DEFAULT_CONSUMER_POLL_FREQUENCY_MS = 1000

val AdminClientAttributeKey = AttributeKey<AdminClient>(KTOR_KAFKA_ADMIN_CLIENT_ATTRIBUTE_KEY)
val ProducerAttributeKey = AttributeKey<KafkaProducer<KafkaRecordKey, GenericRecord>>(KTOR_KAFKA_PRODUCER_ATTRIBUTE_KEY)
val ConsumerAttributeKey = AttributeKey<KafkaConsumer<KafkaRecordKey, GenericRecord>>(KTOR_KAFKA_CONSUMER_ATTRIBUTE_KEY)
// val ConsumerWrapperAttributeKey = AttributeKey<ConsumerWrapper>(KTOR_KAFKA_CONSUMER_WRAPPER_ATTRIBUTE_KEY)

typealias KafkaRecordKey = String
typealias Producer = KafkaProducer<KafkaRecordKey, GenericRecord>
typealias Consumer = KafkaConsumer<KafkaRecordKey, GenericRecord>
typealias ConsumerRecordHandler = suspend Application.(ConsumerRecord<KafkaRecordKey, GenericRecord>) -> Unit
typealias ConsumerFlow = Flow<ConsumerRecord<String, GenericRecord>>
