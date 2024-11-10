package io.github.flaxoos.ktor.server.plugins.kafka

import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.ktor.server.application.install
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import java.io.File
import java.nio.file.Paths

class KafkaSSLIntegrationTest : BaseKafkaIntegrationTest() {
    override val additionalProducerProperties: Map<String, Any> by lazy {
        mapOf(
            // SSL Configuration
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to getKeyStoreFile("kafka.client"),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to PASSWORD,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to PASSWORD,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to getTrustStoreFile("kafka.client"),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to PASSWORD,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
        )
    }

    init {
        val kafkaContainer = KafkaGenericContainer.new()
        val schemaRegistryContainer = SchemaRegistryContainer.new()
        val zooKeeper =
            install(
                ContainerExtension(
                    zookeeperContainer,
                    ContainerLifecycleMode.Project,
                    beforeStart = { generateCertificates() },
                ),
            )
        val kafka =
            install(
                ContainerExtension(
                    kafkaContainer.apply {
                        sslConfig()
                        dependsOn(zooKeeper)
                    },
                    ContainerLifecycleMode.Project,
                    beforeStart = { zooKeeper.start() },
                    beforeTest = {
                        bootstrapServers = BOOTSTRAP_SERVERS
                    },
                ),
            )
        install(
            ContainerExtension(
                schemaRegistryContainer.apply {
                    sslConfig()
                    dependsOn(kafka)
                },
                ContainerLifecycleMode.Project,
                beforeStart = {
                    kafkaContainer.start()
                },
                beforeTest = {
                    resolvedSchemaRegistryUrl = SCHEMA_REGISTRY_URL
                },
            ),
        )

        test("With code configuration") {
            testKafkaApplication(waitSecondsAfterApplicationStart = 10) {
                install(Kafka) {
                    schemaRegistryUrl = resolvedSchemaRegistryUrl
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

    private fun generateCertificates() {
        val script =
            File(
                javaClass
                    .getResource(
                        "/secrets/generate_certificates.sh",
                    )?.toURI() ?: error("File secrets/generate_certificates.sh not found in test resources"),
            )

        ProcessBuilder("sh", script.absolutePath)
            .directory(script.parentFile)
            .inheritIO()
            .start()
            .waitFor()
    }

    private fun getTrustStoreFile(component: String) = getSecretFile("$component.truststore.jks")

    private fun getKeyStoreFile(component: String) = getSecretFile("$component.keystore.jks")

    private fun getSecretFile(fileName: String) =
        javaClass
            .getResource("/secrets/$fileName")
            ?.toURI()
            ?.let { Paths.get(it).toAbsolutePath().toString() }
            ?: throw IllegalArgumentException("Resource $fileName not found")
}
