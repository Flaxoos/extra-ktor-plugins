package io.github.flaxoos.ktor.server.plugins.kafka

import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.kotest.common.ExperimentalKotest
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.ktor.server.application.install
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.testcontainers.containers.KafkaContainer

const val CODE_CONFIGURED_CLIENT_ID = "code-configured-client-id"
const val CODE_CONFIGURED_GROUP_ID = "code-configured-group-id"

@OptIn(ExperimentalKotest::class)
class KafkaIntegrationTest : BaseKafkaIntegrationTest() {
    init {
        val kafkaContainer: KafkaContainer = newKafkaContainer()
        val schemaRegistryContainer = SchemaRegistryContainer.new()
        val kafka =
            install(
                ContainerExtension(
                    kafkaContainer.apply { config() },
                    mode = ContainerLifecycleMode.Project,
                    beforeTest = {
                        bootstrapServers = kafkaContainer.bootstrapServers
                    },
                ),
            )
        install(
            ContainerExtension(
                schemaRegistryContainer.apply { config(kafka) },
                mode = ContainerLifecycleMode.Spec,
                beforeTest = {
                    resolvedSchemaRegistryUrl =
                        "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.firstMappedPort}"
                },
            ),
        )
        context("should pr oduce and consume records").config(timeout = 120.seconds) {
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
                testKafkaApplication {
                    install(Kafka) {
                        schemaRegistryUrl = resolvedSchemaRegistryUrl
                        setupTopics()
                        common { bootstrapServers = listOf(kafka.bootstrapServers) }
                        admin { clientId = CODE_CONFIGURED_CLIENT_ID }
                        producer { clientId = CODE_CONFIGURED_CLIENT_ID }
                        consumer {
                            groupId = CODE_CONFIGURED_GROUP_ID
                        }
                        withConsumerConfig()
                        withRegisterSchemas()
                    }
                }
            }
            test("With code configuration additional configuration") {
                testKafkaApplication {
                    install(Kafka) {
                        schemaRegistryUrl = resolvedSchemaRegistryUrl
                        setupTopics()
                        common {
                            additional {
                                (listOf(kafka.bootstrapServers))
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
}

@Serializable
@AvroName("TestRecord")
@AvroNamespace("io.github.flaxoos")
data class TestRecord(
    val id: Int,
    val topic: String,
)
