package io.github.flaxoos.ktor.server.plugins.kafka

import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.github.flaxoos.ktor.server.plugins.kafka.MessageTimestampType.CreateTime
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.github.flaxoos.ktor.server.plugins.kafka.components.fromRecord
import io.github.flaxoos.ktor.server.plugins.kafka.components.toRecord
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
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
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
class KtorKafkaIntegrationTest : KafkaIntegrationTest() {
    private val logger: Logger = KtorSimpleLogger(javaClass.simpleName)
    private val testTopics = listOf(named("topic1"), named("topic2"))
    private val invocations = 2

    private lateinit var recordChannel: Channel<TestRecord>

    init {
        beforeEach {
            recordChannel = Channel()
        }
        afterEach {
            recordChannel.close()
            revertConfigurationFileEdit()
        }
        context("should produce and consume records").config(timeout = 120.seconds) {
            test("With default config path") {
                editConfigurationFile()
                testKafkaApplication {
                    install(FileConfig.Kafka) {
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
            test("With custom config path") {
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
                editConfigurationFile()
                testKafkaApplication {
                    install(Kafka) {
                        schemaRegistryUrl = super.schemaRegistryUrl
                        setupTopics()
                        common { bootstrapServers = listOf(kafka.bootstrapServers) }
                        admin { clientId = "code-configured-client-id" }
                        producer { clientId = "code-configured-client-id" }
                        consumer { groupId = "code-configured-group-id" }
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
                        fromRecord<TestRecord>(record.value())
                    )
                }
            }
        }
    }

    private fun AbstractKafkaConfig.withRegisterSchemas() {
        topics.forEach {
            registerSchemas(mapOf(TestRecord::class to named(it.name())))
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
        pluginInstallation: Application.() -> Unit
    ) {
        testApplication {
            val client = setupClient()
            environment { config = ApplicationConfig("test-application.conf") }
            setupApplication(extraAssertions) { pluginInstallation() }
            startApplication()
            delay(1.seconds) // let the consumer start polling

            val producedRecords = client.produceRecords()
            val expectedRecords = collectProducedRecords()

            producedRecords shouldContainExactly expectedRecords

            client.clearTopics()
        }
    }

    private suspend fun HttpClient.clearTopics() {
        testTopics.forEach {
            delete(it.value)
        }
    }

    private suspend fun HttpClient.produceRecords() = testTopics.flatMap { topic ->
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
        pluginInstallation: Application.() -> Unit
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
                                deleteTopics(listOf(topic.value))
                            }
                        }
                    }
                }
            }
            extraAssertions(this)
        }
    }

    private fun ApplicationTestBuilder.setupClient(): HttpClient {
        val client = createClient {
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
    val topic: String
)
