package io.flax.ktor.server.plugins

import io.flax.ktor.server.plugins.components.ConsumerShouldRun
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

internal fun kafkaFromConfig(
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
                it.createKafkaTopics(topicBuilders = pluginConfig.topicBuilders) {
                    application.log.info("Created Topics: $first")
                }
            }
        }

    val producer = pluginConfig.producerProperties?.createProducer()
        ?.also {
            application.attributes.put(ProducerAttributeKey, it)
            application.log.info("Kafka producer setup finished")
        }

    val consumer = if (pluginConfig.consumerRecordHandlers.isNotEmpty()) {
        pluginConfig.consumerProperties?.createConsumer()?.also {
            application.attributes.put(ConsumerAttributeKey, it)
            application.attributes.put(ConsumerShouldRun, true)
            application.log.info("Kafka consumer setup finished")
        }
    } else {
        null
    }

    var consumerJob: Job? = null
    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started hook triggered")
        consumerJob = consumer?.let {
            runCatching {
                application.startConsumer(
                    consumer = it,
                    pollFrequency = pluginConfig.consumerPollFrequency,
                    consumerRecordHandlers = pluginConfig.consumerRecordHandlers,
                    consumerOperations = pluginConfig.consumerOperations
                ).also {
                    application.log.info("Started kafka consumer")
                }
            }.onFailure {
                application.log.error("Error starting kafka consumer", it)
            }.getOrNull()
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
            application.attributes.put(ConsumerShouldRun, false)
            consumerJob?.let {
                runBlocking {
                    it.cancel()
                    application.log.info("Cancelled kafka consumer job")
                    // Let it finish one round to avoid "KafkaConsumer is not safe for multi-threaded access"
                    delay(pluginConfig.consumerPollFrequency)
                    consumer?.close()
                }
                application.log.info("Closed kafka consumer")
            }
        }.onFailure {
            application.log.error("Error closing kafka consumer", it)
        }
    }
}

val Application.kafkaAdminClient
    get() = attributes.getOrNull(AdminClientAttributeKey)

val Application.kafkaProducer
    get() = attributes.getOrNull(ProducerAttributeKey)

val Application.kafkaConsumer
    get() = attributes.getOrNull(ConsumerAttributeKey)

//    val consumerWrapper = if (pluginConfig.consumerRecordHandlers.isNotEmpty()) {
//        pluginConfig.consumerProperties?.createConsumer()
//            ?.also { application.attributes.put(ConsumerAttributeKey, it) }?.let {
//                val consumerWrapper = ConsumerWrapper(application, it, pluginConfig.consumerRecordHandlers)
//                consumerWrapper.start()
//                application.attributes.put(ConsumerWrapperAttributeKey, consumerWrapper)
//                consumerWrapper
//            }
//    } else null
