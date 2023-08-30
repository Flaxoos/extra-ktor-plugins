package io.flax.ktor.server.plugins

import com.sksamuel.avro4k.Avro
import io.flax.ktor.server.plugins.Defaults.DEFAULT_CONFIG_PATH
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
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
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val CONFLUENT_PLATFORM_VERSION = "7.5.0"
private const val BOOTSTRAP_SERVERS_PLACEHOLDER = "BOOTSTRAP_SERVERS"
private const val SCHEMA_REGISTRY_URL_PLACEHOLDER = "SCHEMA_REGISTRY_URL"
private const val CONFIG_PATH_PLACEHOLDER = "CONFIG_PATH"
private const val GROUP_ID_PLACEHOLDER = "GROUP_ID"
private const val CLIENT_ID_PLACEHOLDER = "CLIENT_ID"

abstract class KafkaIntegrationTest : FunSpec() {

    abstract val registerSchemas: Map<KClass<out Any>, List<TopicName>>

    private lateinit var applicationConfigFile: File
    private lateinit var applicationConfigFileContent: String
    private val ktorClient = HttpClient { install(ContentNegotiation) { json() } }


    lateinit var schemaRegistryUrl: String

    init {
        beforeSpec {
            kafka.start()
            schemaRegistry.withKafka(kafka).start()
            schemaRegistryUrl = "http://${schemaRegistry.host}:${schemaRegistry.firstMappedPort}"

            registerSchemas.forEach { (klass, topics) ->
                topics.forEach { topicName ->
                    registerSchema(klass, topicName)
                }
            }

            waitTillProducersAccepted(10, 5.seconds)
        }
    }

    @OptIn(InternalSerializationApi::class)
    internal fun registerSchema(klass: KClass<out Any>, topicName: TopicName) {
        val schema = Avro.default.schema(klass.serializer()).toString()
        val payload = mapOf("schema" to schema) // Creating a map to form the payload
        runBlocking {
            ktorClient.post("$schemaRegistryUrl/subjects/$topicName-value/versions") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.let {
                if (!it.status.isSuccess()) {
                    error("${it.status} ${it.bodyAsText()}:\nschema: $payload")
                }
            }
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
                .replace(CLIENT_ID_PLACEHOLDER, this.testCase.name.testName.plus("-client"))
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
                    logger.info("Attempt $i to connect to Kafka broker at bootstrap.servers: $kafka.bootstrapServers failed, retrying")
                    delay(delay)
                }
            }

            producer.close()

            if (!isConnected) {
                throw RuntimeException("Unable to connect to Kafka broker at bootstrap.servers: $kafka.bootstrapServers")
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
