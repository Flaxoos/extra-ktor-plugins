package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.MessageTimestampType.CreateTime
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.github.flaxoos.ktor.server.plugins.kafka.components.fromRecord
import io.github.flaxoos.ktor.server.plugins.kafka.components.toRecord
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.perTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.testcontainers.lifecycle.Startable
import java.io.File
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val KAFKA_BROKER_HOST = "localhost"
private const val KAFKA_BROKER_PORT = 19092
private const val BOOTSTRAP_SERVERS: String = "$KAFKA_BROKER_HOST:$KAFKA_BROKER_PORT"

private const val SCHEMA_REGISTRY_HOST = "localhost"
private const val SCHEMA_REGISTRY_PORT = 8081
private const val SCHEMA_REGISTRY_URL: String = "https://$SCHEMA_REGISTRY_HOST:$SCHEMA_REGISTRY_PORT"

private const val PASSWORD = "test_password"

class KafkaSSLIntegrationTest : BaseKafkaIntegrationTest() {
    override suspend fun beforeStartingContainers() {
        generateCertificates()
    }

    override val containers: List<() -> Startable>
        get() = listOf { dockerComposeContainer }

    override fun provideBootstrapServers(): String =
        dockerComposeContainer
            .getContainerByServiceName(
                "kafka",
            )?.getOrNull()
            ?.let { "${it.host}:$KAFKA_BROKER_PORT" }
            ?: error("No kafka container found")

    override fun provideSchemaRegistryUrl(): String = SCHEMA_REGISTRY_URL

    override val additionalProducerProperties: Map<String, Any> by lazy {
        mapOf(
            // SSL Configuration
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to getKeyStoreFile("kafka.client"),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to "test_password",
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to "test_password",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to getTrustStoreFile("kafka.client"),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to "test_password",
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
        )
    }

    override val httpClient: HttpClient =
        HttpClient {
        }

    init {
        listeners(dockerComposeContainer.perTest())
        beforeSpec {
            generateCertificates()
        }
        test("With code configuration") {
            delay(2.seconds) // wait for Kafka to start
            testKafkaApplication {
                install(Kafka) {
                    schemaRegistryUrl = provideSchemaRegistryUrl()
                    setupTopics()
                    common {
                        bootstrapServers = listOf(BOOTSTRAP_SERVERS)
                        securityProtocol = "SSL"
                    }
                    admin {
                        clientId = CODE_CONFIGURED_CLIENT_ID
                    }
                    producer {
                        clientId = CODE_CONFIGURED_CLIENT_ID
                    }
                    consumer {
                        groupId = CODE_CONFIGURED_GROUP_ID
                    }
                    commonSsl {
                        broker {
                            protocol = "TLSv1.2"
                            endpointIdentificationAlgorithm = ""
                            trustStore {
                                location = getTrustStoreFile("kafka.client")
                                password = PASSWORD
                            }
                            keyStore {
                                location = getKeyStoreFile("kafka.client")
                                password = PASSWORD
                            }
                            keyPassword = PASSWORD
                        }
                        schemaRegistry {
                            protocol = "TLSv1.2"
                            endpointIdentificationAlgorithm = ""
                            trustStore {
                                location = getTrustStoreFile("schemaregistry.client")
                                password = PASSWORD
                            }
                            keyStore {
                                location = getKeyStoreFile("schemaregistry.client")
                                password = PASSWORD
                            }
                            keyPassword = PASSWORD
                        }
                    }
                    withConsumerConfig()
                    withRegisterSchemas()
                }
            }
        }
    }

    private suspend fun generateCertificates() {
        val script =
            File(
                javaClass
                    .getResource(
                        "/secrets/generate_certificates.sh",
                    )?.toURI() ?: error("File secrets/generate_certificates.sh not found in test resources"),
            )
        withContext(Dispatchers.IO) {
            ProcessBuilder("sh", script.absolutePath)
                .directory(script.parentFile)
                .inheritIO()
                .start()
                .waitFor()
        }
    }

    private fun getTrustStoreFile(component: String) = getSecretFile("$component.truststore.jks")

    private fun getKeyStoreFile(component: String) = getSecretFile("$component.keystore.jks")

    private fun getSecretFile(fileName: String) =
        javaClass
            .getResource("/secrets/$fileName")
            ?.toURI()
            ?.let { Paths.get(it).toAbsolutePath().toString() }
            ?: throw IllegalArgumentException("Resource $fileName not found")

    private fun AbstractKafkaConfig.withConsumerConfig() {
        consumerConfig {
            testTopics.forEach { topicName ->
                consumerRecordHandler(topicName) { record ->
                    logger.debug("Consumed record: {} on topic: {}", record, topicName)
                    recordChannel.send(
                        fromRecord<TestRecord>(record.value()),
                    )
                }
            }
        }
    }

    private fun AbstractKafkaConfig.withRegisterSchemas() {
        topics.forEach {
            registerSchemas {
                TestRecord::class at named(it.name())
            }
        }
    }

    private val setupTopics: KafkaConfig.() -> Unit = {
        testTopics.forEach {
            topic(it) {
                partitions = 1
                replicas = 1
                configs {
                    messageTimestampType = CreateTime
                }
            }
        }
    }

    private fun testKafkaApplication(
        extraAssertions: Application.() -> Unit = {},
        pluginInstallation: Application.() -> Unit,
    ) {
        testApplication {
            val client = setupClient()
            environment { applicationConfigFile?.let { config = ApplicationConfig(it.name) } }
            setupApplication(extraAssertions) { pluginInstallation() }
            startApplication()
            delay(10.seconds) // let the consumer start polling

            val producedRecords = client.produceRecords()
            val expectedRecords = collectProducedRecords()
            producedRecords shouldContainExactly expectedRecords

            testTopics.forEach { topicName ->
                val expectedSubject = "${topicName.value}-value"
                logger.info("Making call to check subject version: $expectedSubject")

                client.get("/check-subject-versions") {
                    parameter("subject", expectedSubject)
                }
            }

            client.clearTopics()

            logger.info("Test complete")
        }
    }

    private suspend fun HttpClient.clearTopics() {
        testTopics.forEach {
            delete(it.value)
        }
    }

    private suspend fun HttpClient.produceRecords() =
        testTopics.flatMap { topic ->
            (0.rangeUntil(invocations)).map {
                logger.debug("Triggering record production for topic: $topic")
                get("/$topic").body<TestRecord>()
            }
        }

    private suspend fun collectProducedRecords(): MutableList<TestRecord> {
        val expectedRecords = mutableListOf<TestRecord>()
        repeat(invocations * testTopics.size) {
            expectedRecords.add(recordChannel.receive())
        }
        return expectedRecords
    }

    private fun ApplicationTestBuilder.setupApplication(
        extraAssertions: Application.() -> Unit = {},
        pluginInstallation: Application.() -> Unit,
    ) {
        application {
            install(ContentNegotiation) {
                json()
            }

            pluginInstallation()

            this@application.kafkaAdminClient.shouldNotBeNull()
            this@application.kafkaConsumer.shouldNotBeNull()
            this@application.kafkaProducer.shouldNotBeNull()

            val topicIdCounters = testTopics.associateWith { 0 }
            routing {
                testTopics.forEach { topic ->
                    route("/$topic") {
                        get {
                            val testRecord =
                                TestRecord(topicIdCounters[topic]?.inc() ?: error("topic not counted"), topic.value)
                            val genericRecord = testRecord.toRecord()
                            val record = ProducerRecord(topic.value, "testKey", genericRecord)
                            try {
                                with(call.application.kafkaProducer.shouldNotBeNull()) { send(record) }
                            } catch (e: Exception) {
                                logger.error("Failed to produce record: {}", record)
                            }
                            logger.debug("Produced record: {}", record)
                            call.respond(testRecord)
                        }
                        delete {
                            call.application.kafkaAdminClient
                                .shouldNotBeNull()
                                .deleteTopics(listOf(topic.value))
                                .all()
                                .get()
                        }
                    }
                    get("/check-subject-versions") {
                        retry(maxRetry = 10, timeout = 5.seconds, delay = 500.milliseconds) {
                            logger.info("Checking subject version: ${call.queryParameters["subject"]}")
                            val schemaRegistryResponse =
                                schemaRegistryClient
                                    .shouldNotBeNull()
                                    .client
                                    .get("${super.schemaRegistryUrl}/subjects/${call.queryParameters["subject"]}/versions")

                            schemaRegistryResponse.status shouldBe HttpStatusCode.OK
                            shouldNotThrowAny {
                                schemaRegistryResponse.body<List<Int>>().shouldBe(listOf(1))
                            }
                        }
                    }
                }
            }
            extraAssertions(this)
        }
    }

    private fun ApplicationTestBuilder.setupClient(): HttpClient {
        val client =
            createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
            }
        return client
    }
}
