package io.github.flaxoos.ktor.server.plugins.kafka

import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.github.flaxoos.ktor.server.plugins.kafka.MessageTimestampType.CreateTime
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.github.flaxoos.ktor.server.plugins.kafka.components.fromRecord
import io.github.flaxoos.ktor.server.plugins.kafka.components.toRecord
import io.kotest.common.ExperimentalKotest
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord

const val CODE_CONFIGURED_CLIENT_ID = "code-configured-client-id"
const val CODE_CONFIGURED_GROUP_ID = "code-configured-group-id"

@OptIn(ExperimentalKotest::class)
class KafkaIntegrationTest : BaseKafkaIntegrationTest() {
    override val containers = listOf({ kafkaContainer }, { schemaRegistryContainer.apply { config(kafkaContainer) } })

    override fun provideBootstrapServers(): String = kafkaContainer.bootstrapServers

    override fun provideSchemaRegistryUrl(): String = "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.firstMappedPort}"

    init {
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
        val kafka =
            install(
                ContainerExtension(
                    kafkaContainer.apply { config() },
                    mode = ContainerLifecycleMode.Spec,
                    beforeStart = {},
                    afterStart = {},
                    beforeTest = {
                        bootstrapServers = provideBootstrapServers()
                        waitTillProducersAccepted()
                        recordChannel = Channel()
                    },
                    afterTest = {
                        recordChannel.close()
                        revertConfigurationFileEdit()
                    },
                    beforeShutdown = {},
                    afterShutdown = {
                        afterStoppingContainers()
                    },
                ),
            )
        val schemaRegistry =
            install(
                ContainerExtension(
                    schemaRegistryContainer.apply { config(kafka) },
                    mode = ContainerLifecycleMode.Spec,
                ) {
                    beforeTest {
                        schemaRegistryUrl = provideSchemaRegistryUrl()
                    }
                },
            )
        context("should produce and consume records").config(timeout = 120.seconds) {
            xtest("With default config path") {
                editConfigurationFile()
                testKafkaApplication {
                    install(FileConfig.Kafka) {
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
            xtest("With custom config path") {
                val customConfigPath = "ktor.kafka.config"
                editConfigurationFile(customConfigPath)
                testKafkaApplication {
                    install(FileConfig.Kafka(customConfigPath)) {
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
            test("With code configuration") {
                testKafkaApplication {
                    install(Kafka) {
                        schemaRegistryUrl = provideSchemaRegistryUrl()
                        setupTopics()
                        common { bootstrapServers = listOf(kafka.bootstrapServers) }
                        admin { clientId = CODE_CONFIGURED_CLIENT_ID }
                        producer { clientId = CODE_CONFIGURED_CLIENT_ID }
                        consumer { groupId = CODE_CONFIGURED_GROUP_ID }
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
            xtest("With code configuration additional configuration") {
                testKafkaApplication {
                    install(Kafka) {
                        schemaRegistryUrl = provideSchemaRegistryUrl()
                        setupTopics()
                        common {
                            additional {
                                BOOTSTRAP_SERVERS_CONFIG(listOf(kafka.bootstrapServers))
                            }
                        }
                        admin {
                            additional {
                                CLIENT_ID_CONFIG(CODE_CONFIGURED_CLIENT_ID)
                            }
                        }
                        producer {
                            additional {
                                CLIENT_ID_CONFIG(CODE_CONFIGURED_CLIENT_ID)
                            }
                        }
                        consumer {
                            additional {
                                GROUP_ID_CONFIG(CODE_CONFIGURED_GROUP_ID)
                            }
                        }
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
        }
    }

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
                using {
                    httpClient
                }
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
            delay(1.seconds) // let the consumer start polling

            val producedRecords = client.produceRecords()
            val expectedRecords = collectProducedRecords()
            val expectedSubject = "${testTopics.last().value}.${TestRecord::class.java.simpleName}"
            val response = collectSchemaVersionsBySubject(expectedSubject)

            producedRecords shouldContainExactly expectedRecords
            response.status.value shouldBe 200

            client.clearTopics()
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

    private suspend fun collectSchemaVersionsBySubject(subject: String): HttpResponse =
        httpClient.get("${provideSchemaRegistryUrl()}/subjects/$subject/versions")

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
                            with(call.application.kafkaProducer.shouldNotBeNull()) { send(record) }
                            logger.debug("Produced record: {}", record)
                            call.respond(testRecord)
                        }
                        delete {
                            with(call.application.kafkaAdminClient.shouldNotBeNull()) {
                                deleteTopics(listOf(topic.value)).all().get()
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

@Serializable
@AvroName("TestRecord")
@AvroNamespace("io.github.flaxoos")
data class TestRecord(
    val id: Int,
    val topic: String,
)
