package io.flax.ktor.server.plugins.components

import io.flax.ktor.server.plugins.Consumer
import io.flax.ktor.server.plugins.ConsumerRecordHandler
import io.flax.ktor.server.plugins.KafkaRecordKey
import io.flax.ktor.server.plugins.TopicName
import io.flax.ktor.server.plugins.TopicName.Companion.named
import io.flax.ktor.server.plugins.consumerShouldRun
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal fun Map<String, Any?>.createConsumer(): Consumer =
    KafkaConsumer<KafkaRecordKey, GenericRecord>(this)

internal fun Application.startConsumer(
    consumer: Consumer,
    pollFrequency: Duration,
    consumerRecordHandlers: Map<TopicName, ConsumerRecordHandler>
): Job {
    val consumerFlow = flow {
        consumer.subscribe(consumerRecordHandlers.keys.map { it.value })
        while (consumerShouldRun) {
            log.info("Consumer polling for messages")
            val records = consumer.poll(pollFrequency.toJavaDuration())
            for (record in records) {
                emit(record)
            }
        }
    }
    return launch {
        consumerFlow.collect { record ->
            consumerRecordHandlers[named(record.topic())]?.invoke(this@startConsumer, record)
                ?: log.warn("No handler defined for topic ${record.topic()}")
        }
    }
}

// @Suppress("MemberVisibilityCanBePrivate")
// class ConsumerWrapper(
//    private val application: Application,
//    private val consumer: Consumer,
//    private val pollFrequency: Duration,
//    private val consumerRecordHandlers: Map<TopicName, ConsumerRecordHandler>,
//    private val consumerOperations: List<ConsumerFlow.(Application) -> ConsumerFlow> = emptyList(),
// ) {
//    private val shouldRun = atomic<Boolean>(true)
//    var job: Job? = null
//        private set
//
//    fun start(): Job {
//        val consumerFlow = flow {
//            consumer.subscribe(consumerRecordHandlers.keys.map { it.name })
//            while (shouldRun.value) {
//                val records = consumer.poll(pollFrequency.toJavaDuration())
//                for (record in records) {
//                    emit(record)
//                }
//            }
//        }
//        return application.launch {
//            consumerOperations.fold(consumerFlow) { initial, operation ->
//                operation.invoke(initial, application)
//            }.collect { record ->
//                consumerRecordHandlers[TopicName.named(record.topic())]?.invoke(application, record)
//                    ?: application.log.warn("No handler defined for topic ${record.topic()}")
//            }
//        }
//    }
//
//    suspend fun stop() {
//        shouldRun.update { false }
//        job?.cancel()
//        delay(pollFrequency)
//        consumer.close()
//    }
// }
