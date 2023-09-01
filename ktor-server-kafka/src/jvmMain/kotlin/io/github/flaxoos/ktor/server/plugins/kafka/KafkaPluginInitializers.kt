package io.github.flaxoos.ktor.server.plugins.kafka

import io.ktor.server.application.Application
import io.ktor.server.application.install

/**
 * Install kafka plugin with configuration in the application configuration file
 *
 * @receiver [Application] the ktor server application
 * @param configurationPath The path to the configuration in the application configuration file
 * @param config Configuration block for the plugin, see [KafkaConsumerConfig]
 */
fun Application.installKafkaFromFile(
    configurationPath: String = Defaults.DEFAULT_CONFIG_PATH,
    config: KafkaFileConfig.() -> Unit
) {
    install(Kafka(configurationPath, config))
}

/**
 * Install kafka plugin with configuration in code
 *
 * Example:
 * ```kotlin
 * installKafka {
 *      schemaRegistryUrl = listOf(super.schemaRegistryUrl)
 *      topic(it) {
 *          partitions = 1
 *          replicas = 1
 *          configs {
 *              messageTimestampType = CreateTime
 *          }
 *      }
 *      common { bootstrapServers = listOf("my-kafka") }
 *      admin { } // will create an admin
 *      producer { clientId = "my-client-id" } // will create a producer
 *      consumer { groupId = "my-group-id" } // will create a consumer
 *      consumerConfig {
 *          consumerRecordHandler("my-topic) { record ->
 *              myService.save(record)
 *          )
 *     }
 * }
 * ```
 * @receiver [Application] the ktor server application
 * @param config Configuration block for the plugin, see [KafkaConfig].
 *
 */
fun Application.installKafka(config: KafkaConfig.() -> Unit) {
    install(Kafka) { config() }
}
