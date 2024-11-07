package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.SchemaRegistryContainer.Companion.SCHEMA_REGISTRY_PORT
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private const val CONFLUENT_PLATFORM_VERSION = "7.5.0"
private val logger: Logger = KtorSimpleLogger(BaseKafkaIntegrationTest::class.java.simpleName)
private val kafkaImage: DockerImageName = DockerImageName.parse("confluentinc/cp-kafka:$CONFLUENT_PLATFORM_VERSION")
private val kafkaNetwork = Network.newNetwork()
private const val YML_FILE_PATH = "/Kafka Cluster SSL.yml"
private val yml = object {}::class.java.getResource(YML_FILE_PATH)?.toURI() ?: error("$YML_FILE_PATH not found")

internal val schemaRegistryContainer = SchemaRegistryContainer()
internal val kafkaContainer: KafkaContainer = KafkaContainer(kafkaImage).apply { config() }

internal fun KafkaContainer.config() {
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
    }
    withNetworkAliases("kafka")
    withNetwork(kafkaNetwork)
    withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "false")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
    withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
}

internal fun SchemaRegistryContainer.config(kafka: KafkaContainer) {
    waitingFor(Wait.forHttp("/subjects").forStatusCode(200).withStartupTimeout(120.seconds.toJavaDuration()))
    withExposedPorts(SCHEMA_REGISTRY_PORT)
    withNetwork(kafka.network)
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:$SCHEMA_REGISTRY_PORT")
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://${kafka.networkAliases[0]}:9092")
}

internal val dockerComposeContainer: DockerComposeContainer<*> =
    logErrors { DockerComposeContainer("kafka-ssl", File(yml)) }

private fun <T> logErrors(block: () -> T) =
    runCatching {
        block()
    }.onFailure {
        logger.error(it.message)
    }.getOrThrow()

internal class SchemaRegistryContainer : GenericContainer<SchemaRegistryContainer>("$schemaRegistryImage:$CONFLUENT_PLATFORM_VERSION") {

    internal companion object {
        const val SCHEMA_REGISTRY_PORT = 8081
        val schemaRegistryImage: DockerImageName = DockerImageName.parse("confluentinc/cp-schema-registry")
    }
}

// companion object {
//        protected val logger: Logger = KtorSimpleLogger(BaseKafkaIntegrationTest::class.java.simpleName)
//        private val kafkaLogConsumer = Slf4jLogConsumer(KtorSimpleLogger("kafka")).withSeparateOutputStreams()
//        private val schemaRegistryLogConsumer =
//            Slf4jLogConsumer(KtorSimpleLogger("schemaregistry")).withSeparateOutputStreams()
//
//        private const val KAFKA_IMAGE_NAME = "confluentinc/cp-kafka:$CONFLUENT_PLATFORM_VERSION"
//
//        private fun KafkaContainer.config() {
//            if (System.getProperty("os.name").lowercase().contains("mac")) {
//                withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
//            }
//            withNetworkAliases("kafka")
//            withNetwork(kafkaNetwork)
//            withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "false")
//            withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
//            withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
//        }
//
//        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse(KAFKA_IMAGE_NAME)).apply { config() }
//        internal val schemaRegistry =
//            SchemaRegistryContainer().apply {
//                schemaRegistryConfig()
//            }
//        private val kafkaNetwork = Network.newNetwork()
//
//        private fun SchemaRegistryContainer.schemaRegistryConfig() {
//            if (System.getProperty("os.name").lowercase().contains("mac")) {
//                withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
//            }
//            withNetworkAliases("kafka")
//            withNetwork(kafkaNetwork)
//            withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "false")
//            withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
//            withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
//        }
//
//        private const val YML_FILE_PATH = "/Kafka Cluster SSL.yml"
//        private val yml =
//            this::class.java.getResource(YML_FILE_PATH)?.toURI()
//                ?: error("$YML_FILE_PATH not found")
//        internal val dockerComposeContainer: DockerComposeContainer<*> =
//            logErrors {
//                DockerComposeContainer("kafka-ssl", File(yml))
// //                    .withLogConsumer("kafka", kafkaLogConsumer).withLogConsumer("schemaregistry", schemaRegistryLogConsumer)
//            }
//
//        fun Spec.dockerComposeListener() = listener(dockerComposeContainer.perTest())
//
//        fun Spec.kafkaExtension() = install(ContainerExtension(kafka, mode = ContainerLifecycleMode.Spec)) { config() }
//
//        //                .withLogConsumer(kafkaLogConsumer)
//        fun Spec.schemaRegistryExtension() =
//            install(ContainerExtension(kafka, mode = ContainerLifecycleMode.Spec)) {
//                schemaRegistryConfig()
//            }
//
//        fun <T> logErrors(block: () -> T) =
//            runCatching {
//                block()
//            }.onFailure {
//                logger.error(it.message)
//            }.getOrThrow()
//    }
//
//    class SchemaRegistryContainer : GenericContainer<SchemaRegistryContainer>("$schemaRegistryImage:$CONFLUENT_PLATFORM_VERSION") {
//        init {
//            waitingFor(Wait.forHttp("/subjects").forStatusCode(200).withStartupTimeout(120.seconds.toJavaDuration()))
//            withExposedPorts(SCHEMA_REGISTRY_PORT)
//        }
//
//        fun withKafka(kafka: KafkaContainer): SchemaRegistryContainer = withKafka(kafka.network, "${kafka.networkAliases[0]}:9092")
//
//        private fun withKafka(
//            network: Network?,
//            bootstrapServers: String,
//        ): SchemaRegistryContainer {
//            withNetwork(network)
//            withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
//            withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:$SCHEMA_REGISTRY_PORT")
//            withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://$bootstrapServers")
//            // Uncomment to view the container logs alongside the test logs
// //            withLogConsumer(schemaRegistryLogConsumer)
//            return self()
//        }
//
//        internal companion object {
//            const val SCHEMA_REGISTRY_PORT = 8081
//            val schemaRegistryImage: DockerImageName = DockerImageName.parse("confluentinc/cp-schema-registry")
//        }
//    }

// private val kafkaLogger = KtorSimpleLogger("kafka")
// private val schemaRegistryLogger = KtorSimpleLogger("schemaregistry")
// val kafkaLogConsumer = Slf4jLogConsumer(kafkaLogger).withSeparateOutputStreams()
// val schemaRegistryLogConsumer = Slf4jLogConsumer(schemaRegistryLogger).withSeparateOutputStreams()
