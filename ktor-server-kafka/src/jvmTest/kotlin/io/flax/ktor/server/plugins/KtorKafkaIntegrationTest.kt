package io.flax.ktor.server.plugins

import com.sksamuel.avro4k.Avro
import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.flax.ktor.server.plugins.TopicName.Companion.named
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
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
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.TopicConfig
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds


@OptIn(InternalSerializationApi::class, ExperimentalKotest::class)
class KtorKafkaIntegrationTest : KafkaIntegrationTest() {
    private val logger: Logger = KtorSimpleLogger(javaClass.simpleName)
    private val topics = listOf(named("topic1"), named("topic2"))
    private val invocations = 2
    private lateinit var recordChannel: Channel<TestRecord>
    private lateinit var consumerOperationsCount: AtomicInt

    override val registerSchemas: Map<KClass<out Any>, List<TopicName>> = mapOf(TestRecord::class to topics)

    init {
        beforeEach {
            recordChannel = Channel()
            consumerOperationsCount = atomic(0)
        }
        afterEach {
            recordChannel.close()
            revertConfigurationFileEdit()
        }
        context("should produce and consume records").config(timeout = 120.seconds) {
            test("With default config path") {
                editConfigurationFile()
                testKafkaApplication {
                    installKafka { topicConfig() }
                }
            }
            test("With custom config path") {
                val customConfigPath = "ktor.kafka.config"
                editConfigurationFile(customConfigPath)
                testKafkaApplication {
                    installKafka(configurationPath = customConfigPath) {
                        topicConfig()
                    }
                }
            }
            test("With code configuration") {
                editConfigurationFile()
                testKafkaApplication {
                    installKafkaWith {
                        bootstrapServers = listOf(super.bootstrapServers.value)
                        schemaRegistryUrl = listOf(super.schemaRegistryUrl)
                        topicConfig()
                        admin { }
                        producer {
                            clientId = "code-configured-client-id"
                        }
                        consumer {
                            groupId = "code-configured-group-id"
                        }
                    }
                }
            }
        }
    }

    private val topicConfig: AbstractKafkaConfig.() -> Unit = {
        topics.forEach {
            topic(it) {
                partitions = 1
                replicas = 1
                configs = mapOf(
                    TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG to "CreateTime"
                )
            }
        }
        topics.forEach { topicName ->
            consumerRecordHandler(topicName) { record ->
                logger.info("Consumed record: $record on topic: $topicName")
                recordChannel.send(
                    Avro.default.fromRecord(
                        TestRecord::class.serializer(),
                        record.value()
                    )
                )
            }
        }

        consumerOperations.add { _ ->
            map { record ->
                logger.info("intermediate operation: ${consumerOperationsCount.incrementAndGet()}")
                record
            }
        }
    }

    private fun testKafkaApplication(
        extraAssertions: List<Application.() -> Unit> = emptyList(),
        pluginInstallation: Application.() -> Unit,
    ) {
        testApplication {
            val client = setupClient()
            environment {
                config = ApplicationConfig("test-application.conf")
            }
            setupApplication(extraAssertions) {
                pluginInstallation()
            }

            this.startApplication()

            // let the consumer start polling
            delay(1.seconds)

            client.verifyRecords(recordChannel)
        }
    }

    private suspend fun HttpClient.verifyRecords(
        consumerHandlerChannel: Channel<TestRecord>
    ) {
        val responses = topics.flatMap { topic ->
            (0.rangeUntil(invocations)).map {
                logger.info("Triggering record production for topic: $topic")
                get("/$topic").body<TestRecord>()
            }
        }
        val expectedRecords = mutableListOf<TestRecord>()
        repeat(invocations * topics.size) {
            expectedRecords.add(consumerHandlerChannel.receive())
        }
        responses shouldContainExactly expectedRecords
        consumerOperationsCount.value shouldBe invocations * topics.size
        topics.forEach {
            delete(it.name)
        }
    }

    private fun ApplicationTestBuilder.setupApplication(
        extraAssertions: List<Application.() -> Unit> = emptyList(),
        pluginInstallation: Application.() -> Unit
    ) {
        application {
            install(ContentNegotiation) {
                json()
            }
            environment.monitor.subscribe(ApplicationStopped) { application ->
            }

            pluginInstallation()

            val topicIdCounters = topics.associateWith { atomic(0) }
            routing {
                topics.forEach { topic ->
                    route("/$topic") {
                        get {
                            val testRecord = TestRecord(topicIdCounters[topic]!!.getAndIncrement(), topic.name)
                            val genericRecord =
                                Avro.default.toRecord(
                                    TestRecord::class.serializer(),
                                    testRecord
                                )
                            val record = ProducerRecord(topic.name, "testKey", genericRecord)
                            call.application.kafkaProducer
                                .send(record)
                            logger.info("Produced record: $record")
                            call.respond(testRecord)
                        }
                        delete {
                            call.application.kafkaAdminClient.deleteTopics(listOf(topic.name))
                        }
                    }
                }
            }
            extraAssertions.forEach { assertion -> assertion(this) }
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
@AvroNamespace("io.flax")
data class TestRecord(
    val id: Int,
    val topic: String
)