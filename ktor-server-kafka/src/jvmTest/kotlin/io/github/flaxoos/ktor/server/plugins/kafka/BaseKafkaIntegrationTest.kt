package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CONFIG_PATH
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.ktor.client.HttpClient
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.lifecycle.Startable
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val BOOTSTRAP_SERVERS_PLACEHOLDER = "BOOTSTRAP_SERVERS"
internal const val CONFIG_PATH_PLACEHOLDER = "CONFIG_PATH"
internal const val GROUP_ID_PLACEHOLDER = "GROUP_ID"
internal const val CLIENT_ID_PLACEHOLDER = "CLIENT_ID"
private const val SCHEMA_REGISTRY_URL_PLACEHOLDER = "SCHEMA_REGISTRY_URL"

abstract class BaseKafkaIntegrationTest : FunSpec() {
    open suspend fun beforeStartingContainers() {}

    open fun afterStoppingContainers() {}

    abstract val containers: List<() -> Startable>

    abstract fun provideBootstrapServers(): String

    abstract fun provideSchemaRegistryUrl(): String

    open val additionalProducerProperties: Map<String, Any> = emptyMap()

    protected lateinit var bootstrapServers: String
    protected var applicationConfigFile: File? = null
    private lateinit var originalApplicationConfigFileContent: String
    protected lateinit var schemaRegistryUrl: String
    protected val logger: Logger = KtorSimpleLogger(javaClass.simpleName)
    protected val testTopics = listOf(named("topic1"), named("topic2"))
    protected val invocations = 2
    protected open val httpClient = HttpClient()
    protected lateinit var recordChannel: Channel<TestRecord>

//    init {
//        val startedContainers =
//            mutableListOf<Startable>()
//        beforeEach {
//            containers.forEach {
//                it().let { container ->
//                    container.start()
//                    startedContainers.add(container)
//                }
//            }
//            bootstrapServers = provideBootstrapServers()
//            schemaRegistryUrl = provideSchemaRegistryUrl()
//            waitTillProducersAccepted()
//            recordChannel = Channel()
//        }
//        afterEach {
//            recordChannel.close()
//            startedContainers.reversed().forEach {
//                it.stop()
//            }
//            afterStoppingContainers()
//            revertConfigurationFileEdit()
//        }
//    }

    @Suppress("SwallowedException")
    open suspend fun waitTillProducersAccepted(
        attempts: Int = 10,
        delay: Duration = 5.seconds,
    ) {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

        additionalProducerProperties.forEach { (key, value) ->
            props[key] = value
        }

        val producer = KafkaProducer<String, String>(props)
        var isConnected = false

        logger.info("Waiting to connect to Kafka broker at bootstrap.servers: $bootstrapServers")
        for (i in 0 until attempts) {
            try {
                val record = ProducerRecord("test-topic", "key", "value")
                val futureResult = producer.send(record)
                withContext(Dispatchers.IO) {
                    futureResult.get()
                }
                isConnected = true
                break
            } catch (e: Exception) {
                logger.info(
                    "Attempt $i to connect to Kafka broker at bootstrap.servers: " +
                        "$bootstrapServers failed, retrying",
                )
                delay(delay)
            }
        }

        producer.close()

        if (!isConnected) {
            throw AssertionError(
                "Unable to connect to Kafka broker at bootstrap.servers: $bootstrapServers",
            )
        }
        logger.info("Connected to Kafka broker at bootstrap.servers: $bootstrapServers")
    }

    protected fun revertConfigurationFileEdit() {
        applicationConfigFile?.writeText(originalApplicationConfigFileContent)
    }

    protected fun TestScope.editConfigurationFile(configPath: String = DEFAULT_CONFIG_PATH) {
        applicationConfigFile =
            (
                javaClass.getResource("/test-application.conf")?.toURI()?.let { File(it) }
                    ?: error("Application config file not found")
            ).also {
                originalApplicationConfigFileContent = it.readText()
                it.writeText(
                    originalApplicationConfigFileContent
                        .replace(CONFIG_PATH_PLACEHOLDER, configPath)
                        .replace(BOOTSTRAP_SERVERS_PLACEHOLDER, kafkaContainer.bootstrapServers)
                        .replace(SCHEMA_REGISTRY_URL_PLACEHOLDER, schemaRegistryUrl)
                        .replace(
                            GROUP_ID_PLACEHOLDER,
                            testCase.name.testName.plus("-group"),
                        ).replace(
                            CLIENT_ID_PLACEHOLDER,
                            testCase.name.testName.plus("-client"),
                        ),
                )
            }
    }
}
