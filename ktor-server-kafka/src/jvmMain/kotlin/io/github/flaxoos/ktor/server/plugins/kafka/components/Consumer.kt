package io.github.flaxoos.ktor.server.plugins.kafka.components

import io.github.flaxoos.ktor.server.plugins.kafka.Consumer
import io.github.flaxoos.ktor.server.plugins.kafka.ConsumerRecordHandler
import io.github.flaxoos.ktor.server.plugins.kafka.KafkaRecordKey
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.github.flaxoos.ktor.server.plugins.kafka.consumerShouldRun
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal fun Map<String, Any?>.createConsumer(): Consumer =
    KafkaConsumer<KafkaRecordKey, GenericRecord>(this)

internal fun Application.startConsumer(
    consumer: Consumer,
    pollFrequency: Duration,
    consumerRecordHandlers: Map<TopicName, ConsumerRecordHandler>,
    cleanUp: () -> Unit
): Job {
    val consumerFlow = subscribe(consumer, pollFrequency, consumerRecordHandlers.keys.toList())
    return launch(Dispatchers.IO) {
        try {
            consumerFlow.collect { record ->
                consumerRecordHandlers[named(record.topic())]?.invoke(this@startConsumer, record)
                    ?: log.warn("No handler defined for topic ${record.topic()}")
            }
        } finally {
            withContext(NonCancellable) {
                cleanUp()
            }
        }
    }
}

/**
 * Subscribes a [Consumer] to a list of topics, returning a flow of records
 *
 * @receiver [Application] the ktor server application
 * @param consumer [Consumer] to subscribe
 * @param pollFrequency [Duration] at what frequency should the consumer poll, in practice the timeout passed to [KafkaConsumer.poll]
 * @param topics [List] of topics to subscribe to
 *
 * @return [Flow] of records
 */
fun Application.subscribe(
    consumer: Consumer,
    pollFrequency: Duration,
    topics: List<TopicName>
) = flow {
    consumer.subscribe(topics.map { it.value })
    while (consumerShouldRun) {
        log.debug("Consumer polling for messages")
        val records = withContext(Dispatchers.IO) { consumer.poll(pollFrequency.toJavaDuration()) }
        for (record in records) {
            emit(record)
        }
    }
}
