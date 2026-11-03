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
        val keyPassword =
            sslConfig.keyPassword?.toCharArray() ?: error("Key password must be provided if keystore is used")

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
fun KafkaConfig.commonSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    commonSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the admin client.
 */
fun KafkaConfig.adminSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    adminSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the producer.
 */
fun KafkaConfig.producerSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    producerSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the consumer.
 */
fun KafkaConfig.consumerSsl(configuration: SslPropertiesBuilderPair.() -> Unit) {
    consumerSslPropertiesBuilderPair = SslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SSL configuration for the Schema Registry client.
 */
fun KafkaConfig.schemaRegistryClientSsl(configuration: SslPropertiesBuilder.() -> Unit) {
    schemaRegistryClientSslPropertiesBuilder = SslPropertiesBuilder("schema.registry.").apply(configuration)
}

/**
 * Sets the default SASL configuration for all components.
 */
fun KafkaConfig.commonSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    commonSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the admin client.
 */
fun KafkaConfig.adminSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    adminSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the producer.
 */
fun KafkaConfig.producerSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    producerSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the consumer.
 */
fun KafkaConfig.consumerSasl(configuration: SaslPropertiesBuilderPair.() -> Unit) {
    consumerSaslPropertiesBuilderPair = SaslPropertiesBuilderPair().apply(configuration)
}

/**
 * Sets a specific SASL configuration for the Schema Registry client.
 */
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
    fun broker(configuration: SslPropertiesBuilder.() -> Unit) {
        broker = SslPropertiesBuilder().apply(configuration)
    }

    fun schemaRegistry(configuration: SslPropertiesBuilder.() -> Unit) {
        schemaRegistry = SslPropertiesBuilder("schema.registry.").apply(configuration)
    }
}

@KafkaDsl
data class SaslPropertiesBuilderPair(
    var broker: SaslPropertiesBuilder? = null,
    var schemaRegistry: SaslPropertiesBuilder? = null,
) {
    fun broker(configuration: SaslPropertiesBuilder.() -> Unit) {
        broker = SaslPropertiesBuilder().apply(configuration)
    }

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

    override fun doBuild(): KafkaProperties =
        kafkaPropertiesOf(
            SslConfigs.SSL_PROTOCOL_CONFIG to protocol,
            SslConfigs.SSL_PROVIDER_CONFIG to provider,
            SslConfigs.SSL_CIPHER_SUITES_CONFIG to cipherSuites,
            SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG to enabledProtocols,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to keyStore?.type,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keyStore?.location,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to keyStore?.password,
            SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG to keyStore?.managerAlgorithm,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to trustStore?.type,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to trustStore?.location,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to trustStore?.password,
            SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG to trustStore?.managerAlgorithm,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to keyPassword,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to endpointIdentificationAlgorithm,
            SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG to secureRandomImplementation,
            namespace = namespace,
        )

    fun trustStore(configuration: KeyStorePropertiesBuilder.() -> Unit) {
        trustStore = KeyStorePropertiesBuilder().apply(configuration)
    }

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

    override fun doBuild(): KafkaProperties =
        kafkaPropertiesOf(
            SaslConfigs.SASL_KERBEROS_SERVICE_NAME to kerberosServiceName,
            SaslConfigs.SASL_KERBEROS_KINIT_CMD to kerberosKinitCmd,
            SaslConfigs.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR to kerberosTicketRenewWindowFactor,
            SaslConfigs.SASL_KERBEROS_TICKET_RENEW_JITTER to kerberosTicketRenewJitter,
            SaslConfigs.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN to kerberosMinTimeBeforeRelogin,
            SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_FACTOR to loginRefreshWindowFactor,
            SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_JITTER to loginRefreshWindowJitter,
            SaslConfigs.SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS to loginRefreshMinPeriodSeconds,
            SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS to loginRefreshBufferSeconds,
            SaslConfigs.SASL_MECHANISM to mechanism,
            SaslConfigs.SASL_JAAS_CONFIG to jaasConfig,
            SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS to clientCallbackHandlerClass,
            SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS to loginCallbackHandlerClass,
            SaslConfigs.SASL_LOGIN_CLASS to loginClass,
            namespace = namespace,
        )
}

interface SecurityPropertiesBuilder {
    val namespace: String

    var sslPropertiesBuilder: SslPropertiesBuilder?
    var schemaRegistrySslPropertiesBuilder: SslPropertiesBuilder?

    fun ssl(configuration: SslPropertiesBuilder.() -> Unit = { SslPropertiesBuilder(namespace) }) {
        sslPropertiesBuilder = SslPropertiesBuilder(namespace).apply(configuration)
    }

    fun schemaRegistrySsl(
        configuration: SslPropertiesBuilder.() -> Unit = { SslPropertiesBuilder("schema.registry.") },
    ) {
        schemaRegistrySslPropertiesBuilder = SslPropertiesBuilder("schema.registry.").apply(configuration)
    }

    var saslPropertiesBuilder: SaslPropertiesBuilder?

    fun sasl(configuration: SaslPropertiesBuilder.() -> Unit = { SaslPropertiesBuilder(namespace) }) {
        saslPropertiesBuilder = SaslPropertiesBuilder(namespace).apply(configuration)
    }
}
