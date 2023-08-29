package io.flax.ktor.server.plugins.components

import io.flax.ktor.server.plugins.Consumer
import io.flax.ktor.server.plugins.ConsumerFlow
import io.flax.ktor.server.plugins.ConsumerRecordHandler
import io.flax.ktor.server.plugins.KafkaRecordKey
import io.flax.ktor.server.plugins.TopicName
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.util.AttributeKey
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

internal val ConsumerShouldRun = AttributeKey<Boolean>("ConsumerShouldRun")

internal fun Map<String, Any?>.createConsumer(): Consumer =
    KafkaConsumer<KafkaRecordKey, GenericRecord>(this)

@Suppress("MemberVisibilityCanBePrivate")
class ConsumerWrapper(
    private val application: Application,
    private val consumer: Consumer,
    private val consumerRecordHandlers: Map<TopicName, ConsumerRecordHandler>,
    private val consumerOperations: List<ConsumerFlow.(Application) -> ConsumerFlow> = emptyList(),
    private val pollFrequency: Duration = 100.milliseconds
) {
    private val shouldRun  = atomic<Boolean>(true)
    var job: Job? = null
        private set

    fun start(): Job {
        val consumerFlow = flow {
            consumer.subscribe(consumerRecordHandlers.keys.map { it.name })
            while (true) {
                val records = consumer.poll(pollFrequency.toJavaDuration())
                for (record in records) {
                    emit(record)
                }
            }
        }
        return application.launch {
            consumerOperations.fold(consumerFlow) { initial, operation ->
                operation.invoke(initial, application)
            }.collect { record ->
                consumerRecordHandlers[TopicName.named(record.topic())]?.invoke(application, record)
                    ?: application.log.warn("No handler defined for topic ${record.topic()}")
            }
        }
    }

    suspend fun stop() {
        shouldRun.update { false }
        job?.cancel()
        delay(pollFrequency)
        consumer.close()
    }
}

internal fun Application.startConsumer(
    consumer: Consumer,
    pollFrequency: Duration,
    consumerRecordHandlers: Map<TopicName, ConsumerRecordHandler>,
    consumerOperations: List<ConsumerFlow.(Application) -> ConsumerFlow> = emptyList()
): Job {
    val consumerFlow = flow {
        consumer.subscribe(consumerRecordHandlers.keys.map { it.name })
        while (attributes[ConsumerShouldRun]) {
            log.info("Consumer polling for messages")
            val records = consumer.poll(pollFrequency.toJavaDuration())
            for (record in records) {
                emit(record)
            }
        }
    }
    return launch {
        consumerOperations.fold(consumerFlow) { initial, operation ->
            operation.invoke(initial, this@startConsumer)
        }.collect { record ->
            consumerRecordHandlers[TopicName.named(record.topic())]?.invoke(this@startConsumer, record)
                ?: log.warn("No handler defined for topic ${record.topic()}")
        }
    }
}
