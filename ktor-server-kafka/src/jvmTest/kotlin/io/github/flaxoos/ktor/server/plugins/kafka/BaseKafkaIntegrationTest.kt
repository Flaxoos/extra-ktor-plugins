package io.github.flaxoos.ktor.server.plugins.kafka

import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CONFIG_PATH
import io.github.flaxoos.ktor.server.plugins.kafka.MessageTimestampType.CreateTime
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName.Companion.named
import io.github.flaxoos.ktor.server.plugins.kafka.components.fromRecord
import io.github.flaxoos.ktor.server.plugins.kafka.components.toRecord
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode.Companion.OK
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
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

internal const val BOOTSTRAP_SERVERS_PLACEHOLDER = "BOOTSTRAP_SERVERS"
internal const val CONFIG_PATH_PLACEHOLDER = "CONFIG_PATH"
internal const val GROUP_ID_PLACEHOLDER = "GROUP_ID"
internal const val CLIENT_ID_PLACEHOLDER = "CLIENT_ID"
private const val SCHEMA_REGISTRY_URL_PLACEHOLDER = "SCHEMA_REGISTRY_URL"

abstract class BaseKafkaIntegrationTest : FunSpec() {
    open val additionalProducerProperties: Map<String, Any> = emptyMap()

    protected lateinit var bootstrapServers: String
    protected lateinit var resolvedSchemaRegistryUrl: String
    private var applicationConfigFile: File? = null
    private lateinit var originalApplicationConfigFileContent: String
    private val logger: Logger = KtorSimpleLogger(javaClass.simpleName)
    private val testTopics = listOf(named("topic1"), named("topic2"))
    private val invocations = 2
    protected open val httpClient = HttpClient()
    private lateinit var recordChannel: Channel<TestRecord>

    init {
        beforeEach {
            recordChannel = Channel()
        }
        afterEach {
            recordChannel.close()
            revertConfigurationFileEdit()
        }
    }

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

    private fun revertConfigurationFileEdit() {
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
                        .replace(BOOTSTRAP_SERVERS_PLACEHOLDER, bootstrapServers)
                        .replace(SCHEMA_REGISTRY_URL_PLACEHOLDER, resolvedSchemaRegistryUrl)
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

    protected fun AbstractKafkaConfig.withConsumerConfig() {
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

    protected fun AbstractKafkaConfig.withRegisterSchemas() {
        topics.forEach {
            registerSchemas {
                using {
                    httpClient
                }
                TestRecord::class at named(it.name())
            }
        }
    }

    protected val setupTopics: KafkaConfig.() -> Unit = {
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

    protected fun testKafkaApplication(
        extraAssertions: Application.() -> Unit = {},
        waitSecondsAfterApplicationStart:Int = 1,
        pluginInstallation: Application.() -> Unit,
    ) {
        testApplication {
            val client = setupClient()
            environment { applicationConfigFile?.let { config = ApplicationConfig(it.name) } }
            setupApplication(extraAssertions) { pluginInstallation() }
            waitTillProducersAccepted()
            startApplication()
            delay(waitSecondsAfterApplicationStart.seconds) // let the consumer start polling

            val producedRecords = client.produceRecords()
            val expectedRecords = collectProducedRecords()

            producedRecords shouldContainExactly expectedRecords

            // Check communication with schema registry using the application schema registry client
            testTopics.forEach { topicName ->
                val expectedSubject = "${topicName.value}-value"
                logger.info("Making call to check subject version: $expectedSubject")

                client.get("/check-subject-versions") {
                        parameter("subject", expectedSubject)
                    }.status shouldBe OK
            }

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
                logger.info("Triggering record production for topic: $topic")
                get("/$topic").body<TestRecord>()
            }
        }

    private suspend fun collectProducedRecords(): MutableList<TestRecord> {
        val expectedRecords = mutableListOf<TestRecord>()
        repeat(invocations * testTopics.size) {
            expectedRecords.add(recordChannel.receive().also {
                logger.info("Received record: {}", it)
            })

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


                get("/check-subject-versions") {
                    retry(maxRetry = 10, timeout = 5.seconds, delay = 500.milliseconds) {
                        logger.info("Checking subject version: ${call.queryParameters["subject"]}")
                        val schemaRegistryResponse =
                            schemaRegistryClient
                                .shouldNotBeNull()
                                .client
                                .get("$resolvedSchemaRegistryUrl/subjects/${call.queryParameters["subject"]}/versions")

                        logger.info(
                            "${schemaRegistryResponse.status} response from checking subject version: ${call.queryParameters["subject"]}",
                        )
                        schemaRegistryResponse.status shouldBe OK
                        shouldNotThrowAny {
                            schemaRegistryResponse.body<List<Int>>().shouldBe(listOf(1))
                        }
                        call.respond(OK)
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
