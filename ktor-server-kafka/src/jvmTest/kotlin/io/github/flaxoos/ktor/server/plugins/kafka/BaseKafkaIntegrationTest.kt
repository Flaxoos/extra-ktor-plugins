package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CONFIG_PATH
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val CONFLUENT_PLATFORM_VERSION = "7.5.0"
private const val BOOTSTRAP_SERVERS_PLACEHOLDER = "BOOTSTRAP_SERVERS"
private const val SCHEMA_REGISTRY_URL_PLACEHOLDER = "SCHEMA_REGISTRY_URL"
private const val CONFIG_PATH_PLACEHOLDER = "CONFIG_PATH"
private const val GROUP_ID_PLACEHOLDER = "GROUP_ID"
private const val CLIENT_ID_PLACEHOLDER = "CLIENT_ID"

abstract class BaseKafkaIntegrationTest : FunSpec() {

    private lateinit var applicationConfigFile: File
    private lateinit var applicationConfigFileContent: String

    lateinit var schemaRegistryUrl: String

    init {
        beforeSpec {
            kafka.start()
            schemaRegistry.withKafka(kafka).start()
            schemaRegistryUrl = "http://${schemaRegistry.host}:${schemaRegistry.firstMappedPort}"

            waitTillProducersAccepted(10, 5.seconds)
        }
    }

    protected fun revertConfigurationFileEdit() {
        applicationConfigFile.writeText(applicationConfigFileContent)
    }

    protected fun TestScope.editConfigurationFile(configPath: String = DEFAULT_CONFIG_PATH) {
        applicationConfigFile = javaClass.getResource("/test-application.conf")?.toURI()?.let { File(it) }
            ?: error("Application config file not found")
        applicationConfigFileContent = applicationConfigFile.readText()
        applicationConfigFile.writeText(
            applicationConfigFileContent
                .replace(CONFIG_PATH_PLACEHOLDER, configPath)
                .replace(BOOTSTRAP_SERVERS_PLACEHOLDER, kafka.bootstrapServers)
                .replace(SCHEMA_REGISTRY_URL_PLACEHOLDER, schemaRegistryUrl)
                .replace(GROUP_ID_PLACEHOLDER, this.testCase.name.testName.plus("-group"))
                .replace(CLIENT_ID_PLACEHOLDER, this.testCase.name.testName.plus("-client")),
        )
    }

    protected companion object {
        private val logger = KtorSimpleLogger(this::class.java.simpleName)

        private val kafkaImage: DockerImageName =
            DockerImageName.parse("confluentinc/cp-kafka:$CONFLUENT_PLATFORM_VERSION")
        private val schemaRegistry = SchemaRegistryContainer()
        private val kafkaNetwork = Network.newNetwork()

        val kafka: KafkaContainer = KafkaContainer(kafkaImage).apply {
            if (System.getProperty("os.name").lowercase().contains("mac")) {
                withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
            }
            withNetwork(kafkaNetwork)
            withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "false")
            withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
        }

        @Suppress("SwallowedException")
        suspend fun waitTillProducersAccepted(attempts: Int, delay: Duration) {
            val props = Properties()
            props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
            props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

            val producer = KafkaProducer<String, String>(props)
            var isConnected = false

            logger.info("Waiting to connect to Kafka broker at bootstrap.servers: $kafka.bootstrapServers")
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
                            "$kafka.bootstrapServers failed, retrying",
                    )
                    delay(delay)
                }
            }

            producer.close()

            if (!isConnected) {
                throw AssertionError(
                    "Unable to connect to Kafka broker at bootstrap.servers: " +
                        "$kafka.bootstrapServers",
                )
            }
            logger.info("Connected to Kafka broker at bootstrap.servers: $kafka.bootstrapServers")
        }
    }
}

internal class SchemaRegistryContainer :
    GenericContainer<SchemaRegistryContainer>("$schemaRegistryImage:$CONFLUENT_PLATFORM_VERSION") {

    init {
        waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
        withExposedPorts(SCHEMA_REGISTRY_PORT)
    }

    fun withKafka(kafka: KafkaContainer): SchemaRegistryContainer {
        return withKafka(kafka.network, "${kafka.networkAliases[0]}:9092")
    }

    private fun withKafka(network: Network?, bootstrapServers: String): SchemaRegistryContainer {
        withNetwork(network)
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:$SCHEMA_REGISTRY_PORT")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://$bootstrapServers")
        return self()
    }

    internal companion object {
        const val SCHEMA_REGISTRY_PORT = 8081
        val schemaRegistryImage: DockerImageName = DockerImageName.parse("confluentinc/cp-schema-registry")
    }
}
