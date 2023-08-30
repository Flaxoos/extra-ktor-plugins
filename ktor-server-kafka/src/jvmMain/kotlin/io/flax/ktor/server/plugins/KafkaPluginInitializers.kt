package io.flax.ktor.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install

/**
 * Install kafka plugin with configuration in the application configuration file
 */
fun Application.installKafkaFromFile(
    /**
     * The path to the configuration in the application configuration file
     */
    configurationPath: String = Defaults.DEFAULT_CONFIG_PATH,
    config: KafkaFileConfig.() -> Unit
) {
    install(Kafka(configurationPath, config))
}

/**
 * Install kafka plugin with configuration in code
 */
fun Application.installKafka(config: KafkaConfig.() -> Unit) {
    install(Kafka) { config() }
}
