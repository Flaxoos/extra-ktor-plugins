package io.github.flaxoos.ktor.server.plugins.kafka

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class SslSettings(
    private val sslConfig: SslPropertiesBuilder,
) {
    /**
     * Loads the KeyStore from the provided SSL configuration.
     */
    fun getKeyStore(): KeyStore? = sslConfig.keyStore?.toKeyStore()

    /**
     * Initializes the TrustManagerFactory using the TrustStore properties.
     */
    fun getTrustManagerFactory(): TrustManagerFactory {
        val trustStore = sslConfig.trustStore?.toKeyStore() ?: error("Truststore must be provided")

        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(trustStore)
            }
        return trustManagerFactory
    }

    /**
     * Initializes the KeyManagerFactory using the KeyStore properties.
     */
    fun getKeyManagerFactory(): KeyManagerFactory? {
        val keyStore = getKeyStore() ?: return null
        val keyPassword = sslConfig.keyPassword?.toCharArray() ?: error("Key password must be provided if keystore is used")

        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore, keyPassword)
            }
        return keyManagerFactory
    }

    /**
     * Retrieves the X509TrustManager from the TrustManagerFactory.
     */
    fun getTrustManager(): X509TrustManager {
        val trustManagers = getTrustManagerFactory().trustManagers
        return trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    /**
     * Retrieves the KeyManagers from the KeyManagerFactory.
     */
    fun getKeyManagers(): Array<KeyManager>? {
        val keyManagerFactory = getKeyManagerFactory()
        return keyManagerFactory?.keyManagers
    }
}

/**
 * Sets the default SSL configuration for all components.
 */
@KafkaDsl
fun KafkaConfig.commonSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    commonSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the admin client.
 */
@KafkaDsl
fun KafkaConfig.adminSsl(configuration: SslPropertiesBuilder.() -> Unit) {
    adminSslPropertiesBuilder = SslPropertiesBuilder().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the producer.
 */
@KafkaDsl
fun KafkaConfig.producerSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    producerSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the consumer.
 */
@KafkaDsl
fun KafkaConfig.consumerSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    consumerSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the Schema Registry client.
 */
@KafkaDsl
fun KafkaConfig.schemaRegistryClientSsl(configuration: SslPropertiesBuilder.() -> Unit) {
    schemaRegistryClientSslPropertiesBuilder = SslPropertiesBuilder("schema.registry.").apply(configuration)
}


/**
 * Sets the default SASL configuration for all components.
 */
@KafkaDsl
fun KafkaConfig.commonSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    commonSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the admin client.
 */
@KafkaDsl
fun KafkaConfig.adminSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    adminSaslPropertiesBuilder = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the producer.
 */
@KafkaDsl
fun KafkaConfig.producerSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    producerSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the consumer.
 */
@KafkaDsl
fun KafkaConfig.consumerSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    consumerSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the Schema Registry client.
 */
@KafkaDsl
fun KafkaConfig.schemaRegistryClientSasl(configuration: SaslPropertiesBuilder.() -> Unit) {
    schemaRegistryClientSaslPropertiesBuilder = SaslPropertiesBuilder("schema.registry.").apply(configuration)
}

/**
 * Converts SslConfig to an HttpClient configured with SSL settings.
 */
fun SslPropertiesBuilder.toHttpClient(): HttpClient {
    val sslSettings = SslSettings(this)

    return HttpClient(CIO) {
        engine {
            https {
                trustManager = sslSettings.getTrustManager()
            }
        }
    }
}

@KafkaDsl
class KeyStorePropertiesBuilder {
    var type: String = "JKS"
    var location: String = ""
    var password: String = ""
    var managerAlgorithm: String? = null

    fun toKeyStore(): KeyStore =
        KeyStore.getInstance(type).apply {
            FileInputStream(location).use { fis ->
                load(fis, password.toCharArray())
            }
        }
}

@KafkaDsl
data class SslPropertiesBuilderPair(
    var broker: SslPropertiesBuilder? = null,
    var schemaRegistry: SslPropertiesBuilder? = null,
) {
    @KafkaDsl
    fun broker(configuration: SslPropertiesBuilder.() -> Unit) {
        broker = SslPropertiesBuilder().apply(configuration)
    }

    @KafkaDsl
    fun schemaRegistry(configuration: SslPropertiesBuilder.() -> Unit) {
        schemaRegistry = SslPropertiesBuilder("schema.registry.").apply(configuration)
    }
}

@KafkaDsl
data class SaslPropertiesBuilderPair(
    var broker: SaslPropertiesBuilder? = null,
    var schemaRegistry: SaslPropertiesBuilder? = null,
) {
    @KafkaDsl
    fun broker(configuration: SaslPropertiesBuilder.() -> Unit) {
        broker = SaslPropertiesBuilder().apply(configuration)
    }

    @KafkaDsl
    fun schemaRegistry(configuration: SaslPropertiesBuilder.() -> Unit) {
        schemaRegistry = SaslPropertiesBuilder("schema.registry.").apply(configuration)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
@KafkaDsl
class SslPropertiesBuilder(
    val namespace: String = "",
) : KafkaPropertiesBuilder() {
    var protocol: String? = null
    var provider: String? = null
    var cipherSuites: List<String>? = null
    var enabledProtocols: List<String>? = null

    var trustStore: KeyStorePropertiesBuilder? = null
    var keyStore: KeyStorePropertiesBuilder? = null

    var keyPassword: String? = null
    var endpointIdentificationAlgorithm: String? = null
    var secureRandomImplementation: String? = null

    override fun doBuild(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()
        protocol?.let { configMap[namespace + SslConfigs.SSL_PROTOCOL_CONFIG] = it }
        provider?.let { configMap[namespace + SslConfigs.SSL_PROVIDER_CONFIG] = it }
        cipherSuites?.let { configMap[namespace + SslConfigs.SSL_CIPHER_SUITES_CONFIG] = it }
        enabledProtocols?.let { configMap[namespace + SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG] = it }
        keyStore?.let {
            it.type.let { configMap[namespace + SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = it }
            it.location.let { configMap[namespace + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = it }
            it.password.let { configMap[namespace + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = it }
            it.managerAlgorithm?.let { configMap[namespace + SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG] = it }
        }
        trustStore?.let {
            it.type.let { configMap[namespace + SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = it }
            it.location.let { configMap[namespace + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = it }
            it.password.let { configMap[namespace + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = it }
            it.managerAlgorithm?.let { configMap[namespace + SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG] = it }
        }
        keyPassword?.let { configMap[namespace + SslConfigs.SSL_KEY_PASSWORD_CONFIG] = it }
        endpointIdentificationAlgorithm?.let {
            configMap[ namespace + SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = it
        }
        secureRandomImplementation?.let { configMap[namespace + SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG] = it }
        return configMap
    }

    @KafkaDsl
    fun trustStore(configuration: KeyStorePropertiesBuilder.() -> Unit) {
        trustStore = KeyStorePropertiesBuilder().apply(configuration)
    }

    @KafkaDsl
    fun keyStore(configuration: KeyStorePropertiesBuilder.() -> Unit) {
        keyStore = KeyStorePropertiesBuilder().apply(configuration)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class SaslPropertiesBuilder(
    val namespace: String = "",
) : KafkaPropertiesBuilder() {
    var kerberosServiceName: String? = null
    var kerberosKinitCmd: String? = null
    var kerberosTicketRenewWindowFactor: Double? = null
    var kerberosTicketRenewJitter: Double? = null
    var kerberosMinTimeBeforeRelogin: Long? = null
    var loginRefreshWindowFactor: Double? = null
    var loginRefreshWindowJitter: Double? = null
    var loginRefreshMinPeriodSeconds: Short? = null
    var loginRefreshBufferSeconds: Short? = null
    var mechanism: String? = null
    var jaasConfig: String? = null
    var clientCallbackHandlerClass: String? = null
    var loginCallbackHandlerClass: String? = null
    var loginClass: String? = null

    override fun doBuild(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()
        kerberosServiceName?.let { configMap[namespace + SaslConfigs.SASL_KERBEROS_SERVICE_NAME] = it }
        kerberosKinitCmd?.let { configMap[namespace + SaslConfigs.SASL_KERBEROS_KINIT_CMD] = it }
        kerberosTicketRenewWindowFactor?.let {
            configMap[SaslConfigs.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR] = it
        }
        kerberosTicketRenewJitter?.let { configMap[namespace + SaslConfigs.SASL_KERBEROS_TICKET_RENEW_JITTER] = it }
        kerberosMinTimeBeforeRelogin?.let { configMap[namespace + SaslConfigs.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN] = it }
        loginRefreshWindowFactor?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_FACTOR] = it }
        loginRefreshWindowJitter?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_JITTER] = it }
        loginRefreshMinPeriodSeconds?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS] = it }
        loginRefreshBufferSeconds?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS] = it }
        mechanism?.let { configMap[namespace + SaslConfigs.SASL_MECHANISM] = it }
        jaasConfig?.let { configMap[namespace + SaslConfigs.SASL_JAAS_CONFIG] = it }
        clientCallbackHandlerClass?.let { configMap[namespace + SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS] = it }
        loginCallbackHandlerClass?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS] = it }
        loginClass?.let { configMap[namespace + SaslConfigs.SASL_LOGIN_CLASS] = it }
        return configMap
    }
}

interface SecurityPropertiesBuilder {
    val namespace: String

    var sslPropertiesBuilder: SslPropertiesBuilder?
    var schemaRegistrySslPropertiesBuilder: SslPropertiesBuilder?

    @KafkaDsl
    fun ssl(configuration: SslPropertiesBuilder.() -> Unit = { SslPropertiesBuilder(namespace) }) {
        sslPropertiesBuilder = SslPropertiesBuilder(namespace).apply(configuration)
    }

    @KafkaDsl
    fun schemaRegistrySsl(configuration: SslPropertiesBuilder.() -> Unit = { SslPropertiesBuilder("schema.registry.") }) {
        schemaRegistrySslPropertiesBuilder = SslPropertiesBuilder("schema.registry.").apply(configuration)
    }

    var saslPropertiesBuilder: SaslPropertiesBuilder?

    @KafkaDsl
    fun sasl(configuration: SaslPropertiesBuilder.() -> Unit = { SaslPropertiesBuilder(namespace) }) {
        saslPropertiesBuilder = SaslPropertiesBuilder(namespace).apply(configuration)
    }
}
