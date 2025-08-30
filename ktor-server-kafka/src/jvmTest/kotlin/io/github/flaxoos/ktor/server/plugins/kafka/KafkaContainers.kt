package io.github.flaxoos.ktor.server.plugins.kafka

import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private const val CONFLUENT_PLATFORM_VERSION = "7.7.1"
private const val KAFKA_IMAGE_NAME = "confluentinc/cp-kafka"
private const val SCHEMA_REGISTRY_IMAGE_NAME = "confluentinc/cp-schema-registry"
private const val KAFKA_IMAGE = "$KAFKA_IMAGE_NAME:$CONFLUENT_PLATFORM_VERSION"
private const val SCHEMA_REGISTRY_IMAGE = "$SCHEMA_REGISTRY_IMAGE_NAME:$CONFLUENT_PLATFORM_VERSION"
private val logger: Logger = KtorSimpleLogger(BaseKafkaIntegrationTest::class.java.simpleName)
private val kafkaNetwork = Network.builder().driver("bridge").build()
private const val YML_FILE_PATH = "/Kafka Cluster SSL.yml"
private val yml = object {}::class.java.getResource(YML_FILE_PATH)?.toURI() ?: error("$YML_FILE_PATH not found")
internal const val LOCALHOST = "localhost"
internal const val KAFKA_BROKER_PORT = 19092
internal const val BOOTSTRAP_SERVERS: String = "$LOCALHOST:$KAFKA_BROKER_PORT"
internal const val SCHEMA_REGISTRY_PORT = 8081
internal const val SCHEMA_REGISTRY_URL: String = "https://$LOCALHOST:$SCHEMA_REGISTRY_PORT"
internal const val PASSWORD = "test_password"

internal fun newKafkaContainer() =
    ConfluentKafkaContainer(
        DockerImageName
            .parse(KAFKA_IMAGE)
            .asCompatibleSubstituteFor(KAFKA_IMAGE_NAME),
    ).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT_S.seconds.toJavaDuration())

internal fun ConfluentKafkaContainer.config() {
    withNetwork(kafkaNetwork)
    withNetworkAliases("kafka")
    withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
    withListener("kafka:9095")
}

private const val CONTAINER_STARTUP_TIMEOUT_S = 30

internal fun SchemaRegistryContainer.config(kafka: ConfluentKafkaContainer) {
    waitingFor(
        Wait
            .forHttp("/subjects")
            .forStatusCode(200)
            .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT_S.seconds.toJavaDuration()),
    )
    withExposedPorts(SCHEMA_REGISTRY_PORT)
    withNetwork(kafka.network)
    withNetworkAliases("schema-registry")
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:$SCHEMA_REGISTRY_PORT")
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9095")
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
}

internal val zookeeperContainer =
    GenericContainer("confluentinc/cp-zookeeper:$CONFLUENT_PLATFORM_VERSION").apply {
        withNetwork(kafkaNetwork)
        withNetworkAliases("zookeeper")
        withEnv("ZOOKEEPER_SERVER_ID", "1")
        withEnv("ZOOKEEPER_CLIENT_PORT", "22181")
        withEnv("ZOOKEEPER_TICK_TIME", "2000")
        withEnv("ZOOKEEPER_INIT_LIMIT", "5")
        withEnv("ZOOKEEPER_SYNC_LIMIT", "2")
        withEnv("ZOOKEEPER_SERVERS", "zookeeper:22888:23888")
    }

internal fun KafkaGenericContainer.sslConfig() {
    withNetwork(kafkaNetwork)
    withExposedPorts(KAFKA_BROKER_PORT)
    withFixedExposedPorts(KAFKA_BROKER_PORT to KAFKA_BROKER_PORT)
    withNetworkAliases("kafka")
    withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:22181")
    withEnv("KAFKA_BROKER_ID", "1")
    withEnv("KAFKA_LISTENERS", "EXTERNAL://0.0.0.0:19092,INTERNAL://0.0.0.0:29092")
    withEnv("KAFKA_ADVERTISED_LISTENERS", "EXTERNAL://localhost:19092,INTERNAL://kafka:29092")
    withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "EXTERNAL:SSL,INTERNAL:SSL")
    withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "INTERNAL")

    withEnv("KAFKA_SSL_KEYSTORE_LOCATION", "/etc/kafka/secrets/kafka.server.keystore.jks")
    withEnv("KAFKA_SSL_KEYSTORE_PASSWORD", PASSWORD)
    withEnv("KAFKA_SSL_KEY_PASSWORD", PASSWORD)
    withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", "/etc/kafka/secrets/kafka.server.truststore.jks")
    withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", PASSWORD)
    withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "")

    withEnv("KAFKA_SSL_CLIENT_AUTH", "required")
    withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")

    withCopyToContainer(MountableFile.forClasspathResource("/secrets"), "/etc/kafka/secrets")
}

internal fun SchemaRegistryContainer.sslConfig() {
    withNetwork(kafkaNetwork)
    withExposedPorts(SCHEMA_REGISTRY_PORT)
    withFixedExposedPorts(SCHEMA_REGISTRY_PORT to SCHEMA_REGISTRY_PORT)
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost")
    withEnv("SCHEMA_REGISTRY_LISTENERS", "https://0.0.0.0:8081")

    // Connect to Kafka using the internal listener address without the listener name
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:29092")
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "SSL")

    // SSL configuration for connecting to Kafka (Corrected)
    withEnv(
        "SCHEMA_REGISTRY_KAFKASTORE_SSL_TRUSTSTORE_LOCATION",
        "/etc/kafka/secrets/schemaregistry.server.truststore.jks",
    )
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SSL_TRUSTSTORE_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SSL_KEYSTORE_LOCATION", "/etc/kafka/secrets/schemaregistry.server.keystore.jks")
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SSL_KEYSTORE_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SSL_KEY_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "")

    // SSL configuration for Schema Registry's own listener
    withEnv("SCHEMA_REGISTRY_SSL_TRUSTSTORE_LOCATION", "/etc/kafka/secrets/schemaregistry.server.truststore.jks")
    withEnv("SCHEMA_REGISTRY_SSL_TRUSTSTORE_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_SSL_KEYSTORE_LOCATION", "/etc/kafka/secrets/schemaregistry.server.keystore.jks")
    withEnv("SCHEMA_REGISTRY_SSL_KEYSTORE_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_SSL_KEY_PASSWORD", PASSWORD)
    withEnv("SCHEMA_REGISTRY_SSL_CLIENT_AUTHENTICATION", "NONE")

    withEnv("SCHEMA_REGISTRY_INTER_INSTANCE_PROTOCOL", "https")
    withEnv("SCHEMA_REGISTRY_DEBUG", "true")

    withCopyToContainer(MountableFile.forClasspathResource("/secrets"), "/etc/kafka/secrets")
}

@Suppress("unused")
internal val dockerComposeContainer: DockerComposeContainer<*> =
    logErrors { DockerComposeContainer("kafka-ssl", File(yml)) }

private fun <T> logErrors(block: () -> T) =
    runCatching {
        block()
    }.onFailure {
        logger.error(it.message)
    }.getOrThrow()

internal abstract class FixedPortsContainer<SELF : GenericContainer<SELF>>(
    imageName: String,
) : GenericContainer<SELF>(imageName) {
    fun withFixedExposedPorts(vararg ports: Pair<Int, Int>) =
        apply {
            ports.forEach { (internalPort, externalPort) ->
                addFixedExposedPort(internalPort, externalPort)
            }
        }
}

internal class SchemaRegistryContainer private constructor() : FixedPortsContainer<SchemaRegistryContainer>(SCHEMA_REGISTRY_IMAGE) {
    companion object {
        @Suppress("unused")
        val schemaRegistryImage: DockerImageName = DockerImageName.parse(SCHEMA_REGISTRY_IMAGE)

        fun new() = SchemaRegistryContainer()
    }
}

internal class KafkaGenericContainer private constructor() : FixedPortsContainer<KafkaGenericContainer>(KAFKA_IMAGE) {
    companion object {
        @Suppress("unused")
        val kafkaImage: DockerImageName = DockerImageName.parse(KAFKA_IMAGE)

        fun new() = KafkaGenericContainer()
    }
}

@Suppress("unused")
internal fun ConfluentKafkaContainer.withConsumeLogs() = withLogConsumer(kafkaLogConsumer)

@Suppress("unused")
internal fun KafkaGenericContainer.withConsumeLogs() = withLogConsumer(kafkaLogConsumer)

@Suppress("unused")
internal fun SchemaRegistryContainer.withConsumeLogs() = withLogConsumer(schemaRegistryLogConsumer)

private val kafkaLogger = KtorSimpleLogger("kafka")
private val schemaRegistryLogger = KtorSimpleLogger("schemaregistry")
private val kafkaLogConsumer = Slf4jLogConsumer(kafkaLogger).withSeparateOutputStreams()
private val schemaRegistryLogConsumer = Slf4jLogConsumer(schemaRegistryLogger).withSeparateOutputStreams()
