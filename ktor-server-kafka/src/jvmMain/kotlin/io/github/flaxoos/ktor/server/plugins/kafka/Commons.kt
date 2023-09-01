package io.github.flaxoos.ktor.server.plugins.kafka

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

@JvmInline
value class TopicName(val value: String) {
    companion object {
        infix fun named(name: String) = TopicName(name)
    }
}

object Defaults {
    const val DEFAULT_CLIENT_ID = "ktor-client"
    const val DEFAULT_GROUP_ID = "ktor-group"
    const val DEFAULT_CONFIG_PATH = "ktor.kafka"
    const val DEFAULT_CONSUMER_POLL_FREQUENCY_MS = 1000
    const val DEFAULT_TOPIC_PARTITIONS = 1
    const val DEFAULT_TOPIC_REPLICAS = 1.toShort()
}

object Attributes {
    private const val KTOR_KAFKA_ADMIN_CLIENT_ATTRIBUTE_KEY = "KTOR_KAFKA_ADMIN_CLIENT"
    private const val KTOR_KAFKA_PRODUCER_ATTRIBUTE_KEY = "KTOR_KAFKA_PRODUCER"
    private const val KTOR_KAFKA_CONSUMER_ATTRIBUTE_KEY = "KTOR_KAFKA_CONSUMER"

    /**
     * A
     */
    val AdminClientAttributeKey = AttributeKey<AdminClient>(KTOR_KAFKA_ADMIN_CLIENT_ATTRIBUTE_KEY)
    val ProducerAttributeKey = AttributeKey<KafkaProducer<KafkaRecordKey, GenericRecord>>(
        KTOR_KAFKA_PRODUCER_ATTRIBUTE_KEY
    )
    val ConsumerAttributeKey = AttributeKey<KafkaConsumer<KafkaRecordKey, GenericRecord>>(
        KTOR_KAFKA_CONSUMER_ATTRIBUTE_KEY
    )
}

typealias KafkaRecordKey = String
typealias Producer = KafkaProducer<KafkaRecordKey, GenericRecord>
typealias Consumer = KafkaConsumer<KafkaRecordKey, GenericRecord>
typealias ConsumerRecordHandler = suspend Application.(ConsumerRecord<KafkaRecordKey, GenericRecord>) -> Unit
