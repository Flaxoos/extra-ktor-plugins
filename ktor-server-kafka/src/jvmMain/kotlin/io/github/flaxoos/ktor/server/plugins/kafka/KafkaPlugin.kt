package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.Attributes.AdminClientAttributeKey
import io.github.flaxoos.ktor.server.plugins.kafka.Attributes.ConsumerAttributeKey
import io.github.flaxoos.ktor.server.plugins.kafka.Attributes.ProducerAttributeKey
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CONFIG_PATH
import io.github.flaxoos.ktor.server.plugins.kafka.components.createConsumer
import io.github.flaxoos.ktor.server.plugins.kafka.Attributes.SchemaRegistryClientKey
import io.github.flaxoos.ktor.server.plugins.kafka.components.createConsumer
import io.github.flaxoos.ktor.server.plugins.kafka.components.createKafkaAdminClient
import io.github.flaxoos.ktor.server.plugins.kafka.components.createKafkaTopics
import io.github.flaxoos.ktor.server.plugins.kafka.components.createProducer
import io.github.flaxoos.ktor.server.plugins.kafka.components.createSchemaRegistryClient
import io.github.flaxoos.ktor.server.plugins.kafka.components.startConsumer
import io.github.flaxoos.ktor.server.plugins.kafka.components.startConsumer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.util.AttributeKey
import io.ktor.util.KtorDsl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object FileConfig {
    /**
     * Plugin for setting up a kafka client, configured in application config file
     * Example:
     * ```kotlin
     * install(Kafka) {
     *      consumerConfig {
     *          consumerRecordHandler("my-topic) { record ->
     *              myService.save(record)
     *          )
     *     }
     * }
     * @receiver [Application] the ktor server application
     * @param configurationPath The path to the configuration in the application configuration file
     * @param config Configuration block for the plugin, see [KafkaConsumerConfig]
     */
    val Kafka = createApplicationPlugin(
        name = "Kafka",
        configurationPath = DEFAULT_CONFIG_PATH,
        createConfiguration = ::KafkaFileConfig
    ) {
        setupKafka(pluginConfig)
    }

    @Suppress("FunctionName")
    @KtorDsl
    fun Kafka(configurationPath: String) = createApplicationPlugin(
        name = "Kafka",
        configurationPath = configurationPath,
        createConfiguration = ::KafkaFileConfig
    ) {
        setupKafka(pluginConfig)
    }

    /**
     * Installs the [Kafka] plugin with the given [KafkaFileConfig] block
     */
    @KtorDsl
    fun Application.kafka(
        configurationPath: String = DEFAULT_CONFIG_PATH,
        config: KafkaFileConfig.() -> Unit
    ) {
        install(Kafka(configurationPath)) { config() }
    }
}

/**
 * Plugin for setting up a kafka client
 *
 * Example:
 * ```kotlin
 * install(Kafka) {
 *      schemaRegistryUrl = listOf(super.schemaRegistryUrl)
 *      topic(it) {
 *          partitions = 1
 *          replicas = 1
 *          configs {
 *              messageTimestampType = CreateTime
 *          }
 *      }
 *      common { bootstrapServers = listOf("my-kafka") }
 *      admin { } // will create an admin
 *      producer { clientId = "my-client-id" } // will create a producer
 *      consumer { groupId = "my-group-id" } // will create a consumer
 *      consumerConfig {
 *          consumerRecordHandler("my-topic) { record ->
 *              myService.save(record)
 *          )
 *     }
 * }
 * ```
 */
val Kafka = createApplicationPlugin(
    name = "Kafka",
    createConfiguration = ::KafkaConfig
) {
    setupKafka(pluginConfig)
}

/**
 * Installs the [Kafka] plugin with the given [KafkaConfig] block
 */
@KtorDsl
fun Application.installKafka(config: KafkaConfig.() -> Unit) {
    install(Kafka) { config() }
}

private fun <T : AbstractKafkaConfig> PluginBuilder<T>.setupKafka(pluginConfig: T) {
    application.log.info("Setting up kafka clients")
    pluginConfig.schemaRegistryUrl?.let {
        val schemaRegistryClient = createSchemaRegistryClient(it, pluginConfig.schemaRegistrationTimeoutMs)
        with(application) { schemaRegistryClient.registerSchemas(pluginConfig.schemas) }.also {
            application.attributes.put(SchemaRegistryClientKey, schemaRegistryClient)
        }
    }
    pluginConfig.adminProperties?.createKafkaAdminClient()
        ?.also {
            application.attributes.put(AdminClientAttributeKey, it)
            application.log.info("Kafka admin setup finished")
            runBlocking(Dispatchers.IO) {
                it.createKafkaTopics(topicBuilders = pluginConfig.topics) {
                    application.log.info("Created Topics: $first")
                }
            }
        }

    pluginConfig.producerProperties?.createProducer()
        ?.also {
            application.attributes.put(ProducerAttributeKey, it)
            application.log.info("Kafka producer setup finished")
        }

    pluginConfig.consumerConfig?.let {
        pluginConfig.consumerProperties?.createConsumer()?.also { consumer ->
            application.attributes.put(ConsumerAttributeKey, consumer)
            application.attributes.put(ConsumerShouldRun, true)
            application.log.info("Kafka consumer setup finished")
        }
    }

    onStart()
    onStop()
}

private fun <T : AbstractKafkaConfig> PluginBuilder<T>.onStop() {
    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Application stopped hook triggered")

        runCatching {
            application.kafkaAdminClient?.close()
            application.log.info("Closed kafka admin")
        }.onFailure { application.log.error("Error closing kafka admin", it) }

        runCatching {
            application.kafkaProducer?.close()
            application.log.info("Closed kafka producer")
        }.onFailure { application.log.error("Error closing kafka producer", it) }

        runCatching {
            application.kafkaConsumerJob?.let {
                val consumer = application.kafkaConsumer
                runBlocking(application.coroutineContext) {
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

private fun <T : AbstractKafkaConfig> PluginBuilder<T>.onStart() {
    on(MonitoringEvent(ApplicationStarted)) { application ->
        val consumer = application.kafkaConsumer
        application.log.info("Application started hook triggered")
        if (consumer != null) {
            if (pluginConfig.consumerConfig == null) {
                application.log.warn(
                    "Consumer defined but no consumer configuration defined, " +
                        "make sure to provide one during plugin installation"
                )
            } else {
                with(
                    checkNotNull(pluginConfig.consumerConfig) {
                        "Consumer config changed to null during application start, this shouldn't happen"
                    }
                ) {
                    if (consumerRecordHandlers.isEmpty()) {
                        application.log.debug(
                            "No consumer record handlers defined, " +
                                "consumer job will not start automatically"
                        )
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
    }
}

/**
 * The kafka admin client created by the [Kafka] plugin
 */
val Application.kafkaAdminClient
    get() = attributes.getOrNull(AdminClientAttributeKey)

/**
 * The kafka producer created by the [Kafka] plugin
 */
val Application.kafkaProducer
    get() = attributes.getOrNull(ProducerAttributeKey)

/**
 * The kafka consumer created by the [Kafka] plugin
 */
val Application.kafkaConsumer
    get() = attributes.getOrNull(ConsumerAttributeKey)

/**
 * The schema registry client created by the [Kafka] plugin if the schema registry url is set
 * and schemas to register are set
 */
val Application.schemaRegistryClient
    get() = attributes.getOrNull(SchemaRegistryClientKey)

/**
 * The kafka consumer job created by the [Kafka] plugin
 */
val Application.kafkaConsumerJob
    get() = attributes.getOrNull(ConsumerJob)

internal val Application.consumerShouldRun
    get() = attributes.getOrNull(ConsumerShouldRun) ?: false

internal val ConsumerShouldRun = AttributeKey<Boolean>("ConsumerShouldRun")
internal val ConsumerJob = AttributeKey<Job>("ConsumerJob")
