package io.flax.ktor.server.plugins

import io.flax.ktor.server.plugins.Attributes.AdminClientAttributeKey
import io.flax.ktor.server.plugins.Attributes.ConsumerAttributeKey
import io.flax.ktor.server.plugins.Attributes.ProducerAttributeKey
import io.flax.ktor.server.plugins.components.createConsumer
import io.flax.ktor.server.plugins.components.createKafkaAdminClient
import io.flax.ktor.server.plugins.components.createKafkaTopics
import io.flax.ktor.server.plugins.components.createProducer
import io.flax.ktor.server.plugins.components.startConsumer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Suppress("FunctionName")
internal fun Kafka(
    configurationPath: String,
    additionalConfig: KafkaFileConfig.() -> Unit
) = createApplicationPlugin(
    name = "Kafka",
    configurationPath = configurationPath,
    createConfiguration = ::KafkaFileConfig
) {
    setupKafkaClients(pluginConfig.apply(additionalConfig))
}

internal val Kafka = createApplicationPlugin(
    name = "Kafka",
    createConfiguration = ::KafkaConfig
) {
    setupKafkaClients(pluginConfig)
}

private fun <T : AbstractKafkaConfig> PluginBuilder<T>.setupKafkaClients(pluginConfig: T) {
    application.log.info("Setting up kafka clients")
    val adminClient = pluginConfig.adminProperties?.createKafkaAdminClient()
        ?.also {
            application.attributes.put(AdminClientAttributeKey, it)
            application.log.info("Kafka admin setup finished")
            runBlocking(Dispatchers.IO) {
                it.createKafkaTopics(topicBuilders = pluginConfig.topics) {
                    application.log.info("Created Topics: $first")
                }
            }
        }

    val producer = pluginConfig.producerProperties?.createProducer()
        ?.also {
            application.attributes.put(ProducerAttributeKey, it)
            application.log.info("Kafka producer setup finished")
        }

    val consumer = pluginConfig.consumerConfig?.let {
        pluginConfig.consumerProperties?.createConsumer()?.also {
            application.attributes.put(ConsumerAttributeKey, it)
            application.attributes.put(ConsumerShouldRun, true)
            application.log.info("Kafka consumer setup finished")
        }
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started hook triggered")
        if (consumer != null) {
            if (pluginConfig.consumerConfig == null) {
                application.log.warn("Consumer defined but no consumer configuration defined, make sure to provide one during plugin installation")
            } else {
                with(
                    checkNotNull(pluginConfig.consumerConfig) {
                        "Consumer config changed to null during application start, this shouldn't happen"
                    }
                ) {
                    if (consumerRecordHandlers.isEmpty()) {
                        application.log.debug("No consumer record handlers defined, consumer job will not start automatically")
                    }
                    runCatching {
                        application.startConsumer(
                            consumer = consumer,
                            pollFrequency = consumerPollFrequency,
                            consumerRecordHandlers = consumerRecordHandlers
                        ).also {
                            application.attributes.put(ConsumerJob, it)
                            application.log.info("Started kafka consumer")
                        }
                    }.onFailure {
                        application.log.error("Error starting kafka consumer", it)
                    }
                }
            }
        }

        on(MonitoringEvent(ApplicationStopped)) {
            application.log.info("Application stopped hook triggered")

            runCatching {
                adminClient?.close()
                application.log.info("Closed kafka admin")
            }.onFailure { application.log.error("Error closing kafka admin", it) }

            runCatching {
                producer?.close()
                application.log.info("Closed kafka producer")
            }.onFailure { application.log.error("Error closing kafka producer", it) }

            runCatching {
                application.kafkaConsumerJob?.let {
                    runBlocking {
                        it.cancel()
                        application.log.info("Cancelled kafka consumer job")
                        with(
                            checkNotNull(pluginConfig.consumerConfig) {
                                "Consumer config changed to null during application start, this shouldn't happen"
                            }
                        ) {
                            // Let it finish one round to avoid race condition
                            delay(consumerPollFrequency)
                            consumer?.close()
                        }
                    }
                    application.log.info("Closed kafka consumer")
                }
            }.onFailure {
                application.log.error("Error closing kafka consumer", it)
            }
        }
    }
}

val Application.kafkaAdminClient
    get() = attributes.getOrNull(AdminClientAttributeKey)

val Application.kafkaProducer
    get() = attributes.getOrNull(ProducerAttributeKey)

val Application.kafkaConsumer
    get() = attributes.getOrNull(ConsumerAttributeKey)

val Application.kafkaConsumerJob
    get() = attributes.getOrNull(ConsumerJob)

internal val Application.consumerShouldRun
    get() = attributes.getOrNull(ConsumerShouldRun) ?: false

internal val ConsumerShouldRun = AttributeKey<Boolean>("ConsumerShouldRun")
internal val ConsumerJob = AttributeKey<Job>("ConsumerJob")
