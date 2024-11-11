@file:Suppress("TooManyFunctions")

package io.github.flaxoos.ktor.server.plugins.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CLIENT_ID
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_CONSUMER_POLL_FREQUENCY_MS
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_GROUP_ID
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_SCHEMA_REGISTRY_CLIENT_TIMEOUT_MS
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_TOPIC_PARTITIONS
import io.github.flaxoos.ktor.server.plugins.kafka.Defaults.DEFAULT_TOPIC_REPLICAS
import io.github.flaxoos.ktor.server.plugins.kafka.KafkaConfigPropertiesContext.Companion.propertiesContext
import io.ktor.client.HttpClient
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.logging.KtorSimpleLogger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val Logger = KtorSimpleLogger("KafkaPluginConfig")

@DslMarker
annotation class KafkaDsl

@KafkaDsl
sealed class AbstractKafkaConfig {
    /**
     * The schema registry url, if set, a client will be created and can be accessed later to register schemas manually,
     * if [schemas] is left empty
     */
    abstract val schemaRegistryUrl: String?
    internal abstract val commonProperties: KafkaProperties?
    internal abstract val adminProperties: KafkaProperties?
    internal abstract val producerProperties: KafkaProperties?
    internal abstract val consumerProperties: KafkaProperties?
    internal abstract val topics: List<NewTopic>

    /**
     * Because the consumer is operating in the background, it can be defined in the setup phase
     */
    var consumerConfig: KafkaConsumerConfig? = null

    /**
     * The schemas to register upon startup, if left empty, none will be registered
     */
    internal val schemas: MutableMap<KClass<out Any>, TopicName> = mutableMapOf()

    /**
     * The provider for the client to use to register schemas
     */
    internal var schemaRegistryClientProvider: () -> HttpClient by Delegates.notNull()

    /**
     * Schema registration timeout
     */
    var schemaRegistrationTimeoutMs: Long = DEFAULT_SCHEMA_REGISTRY_CLIENT_TIMEOUT_MS
}

@KafkaDsl
class KafkaConsumerConfig {
    var consumerRecordHandlers: MutableMap<TopicName, ConsumerRecordHandler> = mutableMapOf()
    var consumerPollFrequency: Duration = DEFAULT_CONSUMER_POLL_FREQUENCY_MS.milliseconds
}

class KafkaConfig : AbstractKafkaConfig() {
    override val topics: List<NewTopic> by lazy {
        topicBuilders.map { it.build() }
    }
    override var schemaRegistryUrl: String? = null

    override val commonProperties: KafkaProperties? by lazy {
        commonPropertiesBuilder?.build()?.apply {
            commonSslPropertiesBuilderPair?.let { (broker, schemaRegistry) ->
                broker?.let { putAll(it.build()) }
                schemaRegistry?.let { putAll(it.build()) }
            }
            commonSaslPropertiesBuilderPair?.let { (broker, schemaRegistry) ->
                broker?.let { putAll(it.build()) }
                schemaRegistry?.let { putAll(it.build()) }
            }
        }
    }
    override val adminProperties: KafkaProperties?
        get() =
            adminPropertiesBuilder
                ?.build()
                ?.propertiesContext(this@KafkaConfig)
                ?.withSslProperties(adminSslPropertiesBuilderPair)
                ?.withDefaultAdminConfig()
                ?.delegatingToCommon()
                ?.kafkaProperties

    override val producerProperties: KafkaProperties? by lazy {
        producerPropertiesBuilder
            ?.build()
            ?.propertiesContext(this@KafkaConfig)
            ?.withSslProperties(producerSslPropertiesBuilderPair)
            ?.withSchemaRegistryUrl()
            ?.withDefaultProducerConfig()
            ?.delegatingToCommon()
            ?.kafkaProperties
    }
    override val consumerProperties: KafkaProperties? by lazy {
        consumerPropertiesBuilder
            ?.build()
            ?.propertiesContext(this@KafkaConfig)
            ?.withSslProperties(consumerSslPropertiesBuilderPair)
            ?.withSchemaRegistryUrl()
            ?.withDefaultConsumerConfig()
            ?.delegatingToCommon()
            ?.kafkaProperties
    }

    internal val topicBuilders = mutableListOf<TopicBuilder>()
    internal var commonPropertiesBuilder: CommonClientPropertiesBuilder? = null
    internal var adminPropertiesBuilder: AdminPropertiesBuilder? = null
    internal var producerPropertiesBuilder: ProducerPropertiesBuilder? = null
    internal var consumerPropertiesBuilder: ConsumerPropertiesBuilder? = null

    // SSL Configurations
    internal var commonSslPropertiesBuilderPair: SslPropertiesBuilderPair? = null
    internal var adminSslPropertiesBuilderPair: SslPropertiesBuilderPair? = null
    internal var producerSslPropertiesBuilderPair: SslPropertiesBuilderPair? = null
    internal var consumerSslPropertiesBuilderPair: SslPropertiesBuilderPair? = null
    internal var schemaRegistryClientSslPropertiesBuilder: SslPropertiesBuilder? = null

    // SASL Configurations
    internal var commonSaslPropertiesBuilderPair: SaslPropertiesBuilderPair? = null
    internal var adminSaslPropertiesBuilderPair: SaslPropertiesBuilderPair? = null
    internal var producerSaslPropertiesBuilderPair: SaslPropertiesBuilderPair? = null
    internal var consumerSaslPropertiesBuilderPair: SaslPropertiesBuilderPair? = null
    internal var schemaRegistryClientSaslPropertiesBuilder: SaslPropertiesBuilder? = null
}

/**
 * Configuration for the Kafka plugin
 */
class KafkaFileConfig(
    config: ApplicationConfig,
) : AbstractKafkaConfig() {
    override var schemaRegistryUrl: String? =
        config.propertyOrNull("schema.registry.url")?.getString()

    override val commonProperties: KafkaProperties? =
        config.configOrNull("common")?.toMutableMap()
    override val adminProperties: KafkaProperties? =
        config
            .configOrNull("admin")
            ?.toMutableMap()
            ?.propertiesContext(this@KafkaFileConfig)
            ?.withDefaultAdminConfig()
            ?.delegatingToCommon()
            ?.kafkaProperties
    override val producerProperties: KafkaProperties? =
        config
            .configOrNull("producer")
            ?.toMutableMap()
            ?.propertiesContext(this@KafkaFileConfig)
            ?.withSchemaRegistryUrl()
            ?.withDefaultProducerConfig()
            ?.delegatingToCommon()
            ?.kafkaProperties
    override val consumerProperties: KafkaProperties? =
        config
            .configOrNull("consumer")
            ?.toMutableMap()
            ?.propertiesContext(this@KafkaFileConfig)
            ?.withSchemaRegistryUrl()
            ?.withDefaultConsumerConfig()
            ?.delegatingToCommon()
            ?.kafkaProperties

    override val topics: List<NewTopic> =
        config.configList("topics").map { TopicBuilder.froMap(it.toMap()).build() }
}

private fun ApplicationConfig.configOrNull(name: String) =
    this
        .runCatching { config(name) }
        .getOrNull()
        ?.toMap()
        ?.toMutableMap()

class KafkaConfigPropertiesContext(
    val kafkaConfig: AbstractKafkaConfig,
    val kafkaProperties: KafkaProperties,
) {
    companion object {
        fun KafkaProperties.propertiesContext(kafkaConfig: AbstractKafkaConfig) =
            KafkaConfigPropertiesContext(
                kafkaConfig = kafkaConfig,
                kafkaProperties = this,
            )
    }
}

internal fun KafkaConfigPropertiesContext.delegatingToCommon(): KafkaConfigPropertiesContext =
    apply {
        kafkaConfig.commonProperties?.let {
            kafkaProperties.putAll(it)
        }
    }

internal fun KafkaConfigPropertiesContext.withDefaultAdminConfig() =
    apply {
        kafkaProperties.getOrPut(CommonClientConfigs.CLIENT_ID_CONFIG) { DEFAULT_CLIENT_ID }
    }

internal fun KafkaConfigPropertiesContext.withDefaultProducerConfig() =
    apply {
        kafkaProperties.getOrPut(ProducerConfig.CLIENT_ID_CONFIG) { DEFAULT_CLIENT_ID }
        kafkaProperties.getOrPut(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG) { StringSerializer::class.java.name }
        kafkaProperties.getOrPut(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG) { KafkaAvroSerializer::class.java.name }
    }

internal fun KafkaConfigPropertiesContext.withDefaultConsumerConfig() =
    apply {
        kafkaProperties.getOrPut(ConsumerConfig.GROUP_ID_CONFIG) { DEFAULT_GROUP_ID }
        kafkaProperties.getOrPut(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG) { StringDeserializer::class.java.name }
        kafkaProperties.getOrPut(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG) { KafkaAvroDeserializer::class.java.name }
    }

internal fun KafkaConfigPropertiesContext.withSchemaRegistryUrl() =
    apply {
        kafkaProperties[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = this.kafkaConfig.schemaRegistryUrl
    }

internal fun KafkaConfigPropertiesContext.withSslProperties(properties: SslPropertiesBuilderPair?) =
    apply {
        properties?.let { (broker, schemaRegistry) ->
            broker?.let { kafkaProperties.putAll(it.build()) }
            schemaRegistry?.let { kafkaProperties.putAll(it.build()) }
        }
    }

internal fun KafkaConfigPropertiesContext.withSslProperties(properties: SslPropertiesBuilder?) =
    apply {
        properties?.let { kafkaProperties.putAll(it.build()) }
    }

internal fun KafkaConfigPropertiesContext.withSaslProperties(properties: SaslPropertiesBuilder?) =
    apply {
        properties?.let { kafkaProperties.putAll(it.build()) }
    }

@KafkaDsl
fun KafkaConsumerConfig.consumerRecordHandler(
    topicName: TopicName,
    handler: ConsumerRecordHandler,
) {
    consumerRecordHandlers[topicName] = handler
}

@KafkaDsl
fun KafkaConfig.common(configuration: CommonClientPropertiesBuilder.() -> Unit = { CommonClientPropertiesBuilder }) {
    commonPropertiesBuilder =
        CommonClientPropertiesBuilder.apply(configuration)
}

@KafkaDsl
fun KafkaConfig.admin(configuration: AdminPropertiesBuilder.() -> Unit = { AdminPropertiesBuilder() }) {
    adminPropertiesBuilder = AdminPropertiesBuilder().apply(configuration)
}

@KafkaDsl
fun AbstractKafkaConfig.registerSchemas(configuration: SchemaRegistrationBuilder.() -> Unit = { SchemaRegistrationBuilder() }) {
    SchemaRegistrationBuilder().apply(configuration).let { schemaRegistrationBuilder ->
        schemas.putAll(schemaRegistrationBuilder.schemas)
        schemaRegistryClientProvider = {
            val sslClientProvider =
                if (this is KafkaConfig) {
                    (
                        this.schemaRegistryClientSslPropertiesBuilder
                            ?: this.commonSslPropertiesBuilderPair?.schemaRegistry
                    )?.toHttpClient()
                } else {
                    null
                }
            sslClientProvider?.also {
                Logger.info(
                    "Schema registry client ssl properties provided, so using CIO Http Client as schema registry client, ignoring client provider defined in `SchemaRegistrationBuilder.using`",
                )
            } ?: schemaRegistrationBuilder.clientProvider()
        }
    }
}

@KafkaDsl
fun KafkaConfig.topic(
    name: TopicName,
    block: TopicBuilder.() -> Unit,
) {
    topicBuilders.add(TopicBuilder(name).apply(block))
}

@KafkaDsl
fun KafkaConfig.producer(
    configuration: ProducerPropertiesBuilder.() -> Unit = {
        ProducerPropertiesBuilder(
            schemaRegistryUrl,
        )
    },
) {
    producerPropertiesBuilder =
        ProducerPropertiesBuilder(
            // assuming only avro is used, support custom serializers later
            with(checkNotNull(schemaRegistryUrl) { "Consumer schema registry url is not set" }) {
                this
            },
        ).apply(configuration)
}

@KafkaDsl
fun KafkaConfig.consumer(
    configuration: ConsumerPropertiesBuilder.() -> Unit = {
        ConsumerPropertiesBuilder(
            schemaRegistryUrl,
        )
    },
) {
    consumerPropertiesBuilder =
        ConsumerPropertiesBuilder(
            // assuming only avro is used, support custom serializers later
            with(checkNotNull(schemaRegistryUrl) { "Consumer schema registry url is not set" }) {
                this
            },
        ).apply(configuration)
}

@KafkaDsl
fun AbstractKafkaConfig.consumerConfig(configuration: KafkaConsumerConfig.() -> Unit = { }) {
    consumerConfig = KafkaConsumerConfig().apply(configuration)
}

@KafkaDsl
class SchemaRegistrationBuilder {
    internal val schemas: MutableMap<KClass<out Any>, TopicName> = mutableMapOf()
    internal var clientProvider: () -> HttpClient = {
        HttpClient()
    }

    infix fun KClass<out Any>.at(topicName: TopicName) {
        schemas[this] = topicName
    }

    /**
     * optionally provide a client to register schemas, by default, CIO would be used.
     * In any case, the following it would be configured with serialization json and the configured
     * [AbstractKafkaConfig.schemaRegistrationTimeoutMs]
     */
    @KafkaDsl
    fun using(provider: () -> HttpClient) {
        clientProvider = provider
    }
}

@KafkaDsl
@Suppress("MemberVisibilityCanBePrivate")
class TopicBuilder(
    internal val name: TopicName,
) {
    var partitions: Int = DEFAULT_TOPIC_PARTITIONS
    var replicas: Short = DEFAULT_TOPIC_REPLICAS
    var replicasAssignments: Map<Int, List<Int?>>? = null
    internal var configs: Map<String, Any?>? = null

    @KafkaDsl
    fun configs(config: TopicPropertiesBuilder.() -> Unit) {
        configs = TopicPropertiesBuilder().apply(config).build()
    }

    internal fun build(): NewTopic {
        val topic =
            if (replicasAssignments == null) {
                NewTopic(name.value, partitions, replicas)
            } else {
                NewTopic(name.value, replicasAssignments)
            }
        return topic.configs(configs?.filterValues { it != null }?.mapValues { it.value.toString() })
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun froMap(map: Map<String, Any?>): TopicBuilder =
            TopicBuilder(TopicName.named(map["name"] as String)).apply {
                partitions = map["partitions"] as Int
                replicas = (map["replicas"] as Int).toShort()
                replicasAssignments = map["replicasAssignments"] as Map<Int, List<Int?>>?
                configs = map["configs"] as Map<String, Any?>?
            }
    }
}

@KafkaDsl
class AdditionalKafkaProperties {
    internal val map = mutableMapOf<String, Any?>()

    @KafkaDsl
    operator fun String.invoke(value: Any?) {
        map[this] = value
    }
}

/**
 * [KafkaDsl] Builder for [KafkaProperties]
 */
@KafkaDsl
sealed class KafkaPropertiesBuilder {
    private val additionalKafkaProperties = AdditionalKafkaProperties()

    fun build(): KafkaProperties {
        val kafkaProperties = doBuild()
        kafkaProperties.putAll(additionalKafkaProperties.map)
        return kafkaProperties
    }

    abstract fun doBuild(): KafkaProperties

    @KafkaDsl
    fun additional(config: AdditionalKafkaProperties.() -> Unit) {
        additionalKafkaProperties.config()
    }
}

/**
 * See [TopicConfig]
 */
@Suppress("MemberVisibilityCanBePrivate", "CyclomaticComplexMethod")
class TopicPropertiesBuilder : KafkaPropertiesBuilder() {
    var segmentBytes: Int? = null
    var segmentMs: Long? = null
    var segmentJitterMs: Long? = null
    var segmentIndexBytes: Int? = null
    var flushMessagesInterval: Long? = null
    var flushMs: Long? = null
    var retentionBytes: Long? = null
    var retentionMs: Long? = null
    var maxMessageBytes: Int? = null
    var indexIntervalBytes: Int? = null
    var fileDeleteDelayMs: Long? = null
    var deleteRetentionMs: Long? = null
    var minCompactionLagMs: Long? = null
    var maxCompactionLagMs: Long? = null
    var minCleanableDirtyRatio: Float? = null
    var cleanupPolicy: String? = null
    var uncleanLeaderElectionEnable: Boolean? = null
    var minInSyncReplicas: Int? = null
    var compressionType: CompressionType? = null
    var preallocate: Boolean? = null

    @Deprecated("see [TopicConfig.MESSAGE_FORMAT_VERSION_CONFIG]")
    var messageFormatVersion: String? = null
    var messageTimestampType: MessageTimestampType? = null

    @Deprecated("see [TopicConfig.MESSAGE_TIMESTAMP_DIFFERENCE_MAX_MS_CONFIG]")
    var messageTimestampDifferenceMaxMs: Long? = null
    var messageDownconversionEnable: Boolean? = null

    override fun doBuild(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()

        segmentBytes?.let { configMap[TopicConfig.SEGMENT_BYTES_CONFIG] = it }
        segmentMs?.let { configMap[TopicConfig.SEGMENT_MS_CONFIG] = it }
        segmentJitterMs?.let { configMap[TopicConfig.SEGMENT_JITTER_MS_CONFIG] = it }
        segmentIndexBytes?.let { configMap[TopicConfig.SEGMENT_INDEX_BYTES_CONFIG] = it }
        flushMessagesInterval?.let { configMap[TopicConfig.FLUSH_MESSAGES_INTERVAL_CONFIG] = it }
        flushMs?.let { configMap[TopicConfig.FLUSH_MS_CONFIG] = it }
        retentionBytes?.let { configMap[TopicConfig.RETENTION_BYTES_CONFIG] = it }
        retentionMs?.let { configMap[TopicConfig.RETENTION_MS_CONFIG] = it }
        maxMessageBytes?.let { configMap[TopicConfig.MAX_MESSAGE_BYTES_CONFIG] = it }
        indexIntervalBytes?.let { configMap[TopicConfig.INDEX_INTERVAL_BYTES_CONFIG] = it }
        fileDeleteDelayMs?.let { configMap[TopicConfig.FILE_DELETE_DELAY_MS_CONFIG] = it }
        deleteRetentionMs?.let { configMap[TopicConfig.DELETE_RETENTION_MS_CONFIG] = it }
        minCompactionLagMs?.let {
            configMap[
                TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG,
            ] = it
        }
        maxCompactionLagMs?.let {
            configMap[
                TopicConfig.MAX_COMPACTION_LAG_MS_CONFIG,
            ] = it
        }
        minCleanableDirtyRatio?.let {
            configMap[
                TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG,
            ] = it
        }
        cleanupPolicy?.let { configMap[TopicConfig.CLEANUP_POLICY_CONFIG] = it }
        uncleanLeaderElectionEnable?.let {
            configMap[
                TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG,
            ] = it
        }
        minInSyncReplicas?.let {
            configMap[
                TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
            ] = it
        }
        compressionType?.let {
            configMap[
                TopicConfig.COMPRESSION_TYPE_CONFIG,
            ] = it
        }
        preallocate?.let {
            configMap[
                TopicConfig.PREALLOCATE_CONFIG,
            ] = it
        }

        @Suppress("DEPRECATION")
        messageFormatVersion?.let { configMap[TopicConfig.MESSAGE_FORMAT_VERSION_CONFIG] = it }
        messageTimestampType?.let { configMap[TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG] = it }
        @Suppress("DEPRECATION")
        messageTimestampDifferenceMaxMs?.let {
            configMap[
                TopicConfig.MESSAGE_TIMESTAMP_DIFFERENCE_MAX_MS_CONFIG,
            ] = it
        }
        messageDownconversionEnable?.let {
            configMap[
                TopicConfig.MESSAGE_DOWNCONVERSION_ENABLE_CONFIG,
            ] = it
        }
        return configMap
    }
}

/**
 * see [CommonClientConfigs]
 */
@Suppress("MemberVisibilityCanBePrivate", "CyclomaticComplexMethod")
sealed class ClientPropertiesBuilder : KafkaPropertiesBuilder() {
    var bootstrapServers: Any? = null
    var clientDnsLookup: Any? = null
    var metadataMaxAge: Any? = null
    var sendBuffer: Any? = null
    var receiveBuffer: Any? = null
    var clientId: Any? = null
    var clientRack: Any? = null
    var reconnectBackoffMs: Any? = null
    var reconnectBackoffMaxMs: Any? = null
    var retries: Any? = null
    var retryBackoffMs: Any? = null
    var metricsSampleWindowMs: Any? = null
    var metricsNumSamples: Any? = null
    var metricsRecordingLevel: Any? = null
    var metricReporterClasses: Any? = null
    var securityProtocol: Any? = null
    var connectionsMaxIdleMs: Any? = null
    var requestTimeoutMs: Any? = null

    internal fun buildCommon(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()
        bootstrapServers?.let { configMap[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = it }
        clientDnsLookup?.let { configMap[CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG] = it }
        metadataMaxAge?.let { configMap[CommonClientConfigs.METADATA_MAX_AGE_CONFIG] = it }
        sendBuffer?.let { configMap[CommonClientConfigs.SEND_BUFFER_CONFIG] = it }
        receiveBuffer?.let { configMap[CommonClientConfigs.RECEIVE_BUFFER_CONFIG] = it }
        clientId?.let { configMap[CommonClientConfigs.CLIENT_ID_CONFIG] = it }
        clientRack?.let { configMap[CommonClientConfigs.CLIENT_RACK_CONFIG] = it }
        reconnectBackoffMs?.let { configMap[CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG] = it }
        reconnectBackoffMaxMs?.let { configMap[CommonClientConfigs.RECONNECT_BACKOFF_MAX_MS_CONFIG] = it }
        retries?.let { configMap[CommonClientConfigs.RETRIES_CONFIG] = it }
        retryBackoffMs?.let { configMap[CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG] = it }
        metricsSampleWindowMs?.let { configMap[CommonClientConfigs.METRICS_SAMPLE_WINDOW_MS_CONFIG] = it }
        metricsNumSamples?.let { configMap[CommonClientConfigs.METRICS_NUM_SAMPLES_CONFIG] = it }
        metricsRecordingLevel?.let { configMap[CommonClientConfigs.METRICS_RECORDING_LEVEL_CONFIG] = it }
        metricReporterClasses?.let { configMap[CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG] = it }
        securityProtocol?.let { configMap[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = it }
        connectionsMaxIdleMs?.let { configMap[CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG] = it }
        requestTimeoutMs?.let { configMap[CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG] = it }
        return configMap
    }

    override fun doBuild() = buildCommon()
}

/**
 * Concrete implementation of [ClientPropertiesBuilder] to represent the common properties
 */
data object CommonClientPropertiesBuilder : ClientPropertiesBuilder()

class AdminPropertiesBuilder : ClientPropertiesBuilder()

/**
 * Used to constraint consumer and producer builders to provide a schema registry url
 */
internal interface SchemaRegistryProvider {
    var schemaRegistryUrl: String?
}

/**
 * see [ProducerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection", "CyclomaticComplexMethod")
class ProducerPropertiesBuilder(
    override var schemaRegistryUrl: String? = null,
) : ClientPropertiesBuilder(),
    SchemaRegistryProvider {
    var batchSize: Any? = null
    var acks: Any? = null
    var lingerMs: Any? = null
    var deliveryTimeoutMs: Any? = null
    var maxRequestSize: Any? = null
    var maxBlockMs: Any? = null
    var bufferMemory: Any? = null
    var compressionType: CompressionType? = null
    var maxInFlightRequestsPerConnection: Any? = null
    var keySerializerClass: Any? = null
    var valueSerializerClass: Any? = null
    var partitionerClass: Any? = null
    var interceptorClasses: Any? = null
    var enableIdempotence: Any? = null
    var transactionTimeout: Any? = null
    var transactionalId: Any? = null

    private var serializerPropertiesBuilder: KafkaAvroSerializerPropertiesBuilder? = null

    /**
     * This is not yet tested but should work as it just does the same thing all the config function do which is to add these properties to the config map
     */
    @KafkaDsl
    fun serializer(configuration: KafkaAvroSerializerPropertiesBuilder.() -> Unit = { KafkaAvroSerializerPropertiesBuilder() }) {
        serializerPropertiesBuilder = KafkaAvroSerializerPropertiesBuilder().apply(configuration)
    }

    override fun doBuild(): KafkaProperties {
        val configMap = buildCommon()
        batchSize?.let { configMap[ProducerConfig.BATCH_SIZE_CONFIG] = it }
        acks?.let { configMap[ProducerConfig.ACKS_CONFIG] = it }
        lingerMs?.let { configMap[ProducerConfig.LINGER_MS_CONFIG] = it }
        deliveryTimeoutMs?.let { configMap[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = it }
        maxRequestSize?.let { configMap[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = it }
        maxBlockMs?.let { configMap[ProducerConfig.MAX_BLOCK_MS_CONFIG] = it }
        bufferMemory?.let { configMap[ProducerConfig.BUFFER_MEMORY_CONFIG] = it }
        compressionType?.let { configMap[ProducerConfig.COMPRESSION_TYPE_CONFIG] = it }
        maxInFlightRequestsPerConnection?.let {
            configMap[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = it
        }
        keySerializerClass?.let { configMap[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = it }
        valueSerializerClass?.let { configMap[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = it }
        partitionerClass?.let { configMap[ProducerConfig.PARTITIONER_CLASS_CONFIG] = it }
        interceptorClasses?.let { configMap[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = it }
        enableIdempotence?.let { configMap[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = it }
        transactionTimeout?.let { configMap[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = it }
        transactionalId?.let { configMap[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = it }

        return configMap
            .apply {
                serializerPropertiesBuilder?.let { serializer -> putAll(serializer.build()) }
            }
    }
}

/**
 * see [ConsumerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate", "CyclomaticComplexMethod")
class ConsumerPropertiesBuilder(
    override var schemaRegistryUrl: String? = null,
) : ClientPropertiesBuilder(),
    SchemaRegistryProvider {
    var groupId: Any? = null
    var groupInstanceId: Any? = null
    var maxPollRecords: Any? = null
    var maxPollIntervalMs: Any? = null
    var sessionTimeoutMs: Any? = null
    var heartbeatIntervalMs: Any? = null
    var enableAutoCommit: Any? = null
    var autoCommitIntervalMs: Any? = null
    var partitionAssignmentStrategy: Any? = null
    var autoOffsetReset: Any? = null
    var fetchMinBytes: Any? = null
    var fetchMaxBytes: Any? = null
    var fetchMaxWaitMs: Any? = null
    var maxPartitionFetchBytes: Any? = null
    var checkCrcs: Any? = null
    var keyDeserializerClass: Any? = null
    var valueDeserializerClass: Any? = null
    var defaultApiTimeoutMs: Any? = null
    var interceptorClasses: Any? = null
    var excludeInternalTopics: Any? = null
    var isolationLevel: Any? = null
    var allowAutoCreateTopics: Any? = null

    var deserializerPropertiesBuilder: KafkaAvroDeserializerPropertiesBuilder? = null

    /**
     * This is not yet tested but should work as it just does the same thing all the config function do which is to add these properties to the config map
     */
    @KafkaDsl
    fun deserializer(configuration: KafkaAvroDeserializerPropertiesBuilder.() -> Unit = { KafkaAvroDeserializerPropertiesBuilder() }) {
        deserializerPropertiesBuilder = KafkaAvroDeserializerPropertiesBuilder().apply(configuration)
    }

    override fun doBuild(): KafkaProperties {
        val configMap = buildCommon()
        groupId?.let { configMap[ConsumerConfig.GROUP_ID_CONFIG] = it }
        groupInstanceId?.let { configMap[ConsumerConfig.GROUP_INSTANCE_ID_CONFIG] = it }
        maxPollRecords?.let { configMap[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = it }
        maxPollIntervalMs?.let { configMap[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = it }
        sessionTimeoutMs?.let { configMap[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = it }
        heartbeatIntervalMs?.let { configMap[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = it }
        enableAutoCommit?.let { configMap[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = it }
        autoCommitIntervalMs?.let { configMap[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = it }
        partitionAssignmentStrategy?.let { configMap[ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG] = it }
        autoOffsetReset?.let { configMap[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = it }
        fetchMinBytes?.let { configMap[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] = it }
        fetchMaxBytes?.let { configMap[ConsumerConfig.FETCH_MAX_BYTES_CONFIG] = it }
        fetchMaxWaitMs?.let { configMap[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] = it }
        maxPartitionFetchBytes?.let { configMap[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = it }
        checkCrcs?.let { configMap[ConsumerConfig.CHECK_CRCS_CONFIG] = it }
        keyDeserializerClass?.let { configMap[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = it }
        valueDeserializerClass?.let { configMap[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = it }
        defaultApiTimeoutMs?.let { configMap[ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = it }
        interceptorClasses?.let { configMap[ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG] = it }
        excludeInternalTopics?.let { configMap[ConsumerConfig.EXCLUDE_INTERNAL_TOPICS_CONFIG] = it }
        isolationLevel?.let { configMap[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = it }
        allowAutoCreateTopics?.let { configMap[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = it }

        return configMap
            .apply {
                deserializerPropertiesBuilder?.let { deserializer -> putAll(deserializer.build()) }
            }
    }
}

/**
 * see [AbstractKafkaSchemaSerDeConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class KafkaAvroSerDePropertiesBuilder : KafkaPropertiesBuilder() {
    var schemaRegistryUrl: List<String>? = null
    var maxSchemasPerSubject: Int? = null
    var normalizeSchemas: Boolean? = null
    var autoRegisterSchemas: Boolean? = null
    var propagateSchemaTags: Boolean? = null
    var useSchemaId: Int? = null
    var idCompatibilityStrict: Boolean? = null
    var useLatestVersion: Boolean? = null
    var latestCompatibilityStrict: Boolean? = null
    var latestCacheSize: Int? = null
    var latestCacheTtl: Int? = null
    var useLatestWithMetadata: String? = null
    var schemaFormat: String? = null
    var ruleExecutors: List<String>? = null
    var ruleActions: List<String>? = null
    var ruleServiceLoaderEnable: Boolean? = null
    var basicAuthCredentialsSource: String? = null
    var userInfoConfig: String? = null
    var bearerAuthToken: String? = null
    var contextNameStrategy: String? = null
    var keySubjectNameStrategy: String? = null
    var valueSubjectNameStrategy: String? = null
    var schemaReflection: Boolean? = null
    var proxyHost: String? = null
    var proxyPort: Int? = null

    internal fun buildCommon(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()
        schemaRegistryUrl?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = it
        }
        maxSchemasPerSubject?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_CONFIG] = it
        }
        normalizeSchemas?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.NORMALIZE_SCHEMAS] = it
        }
        autoRegisterSchemas?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS] = it
        }
        propagateSchemaTags?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.PROPAGATE_SCHEMA_TAGS] = it
        }
        useSchemaId?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.USE_SCHEMA_ID] = it
        }
        idCompatibilityStrict?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.ID_COMPATIBILITY_STRICT] = it
        }
        useLatestVersion?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION] = it
        }
        latestCompatibilityStrict?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT] = it
        }
        latestCacheSize?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.LATEST_CACHE_SIZE] = it
        }
        latestCacheTtl?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.LATEST_CACHE_TTL] = it
        }
        useLatestWithMetadata?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.USE_LATEST_WITH_METADATA] = it
        }
        schemaFormat?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.SCHEMA_FORMAT] = it
        }
        ruleExecutors?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.RULE_EXECUTORS] = it
        }
        ruleActions?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.RULE_ACTIONS] = it
        }
        ruleServiceLoaderEnable?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.RULE_SERVICE_LOADER_ENABLE] = it
        }
        basicAuthCredentialsSource?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = it
        }
        userInfoConfig?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG] = it
        }
        bearerAuthToken?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.BEARER_AUTH_TOKEN_CONFIG] = it
        }
        contextNameStrategy?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.CONTEXT_NAME_STRATEGY] = it
        }
        keySubjectNameStrategy?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY] = it
        }
        valueSubjectNameStrategy?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY] = it
        }
        schemaReflection?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.SCHEMA_REFLECTION_CONFIG] = it
        }
        proxyHost?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.PROXY_HOST] = it
        }
        proxyPort?.let {
            configMap[AbstractKafkaSchemaSerDeConfig.PROXY_PORT] = it
        }

        return configMap
    }

    override fun doBuild(): KafkaProperties = buildCommon()
}

/**
 * see [KafkaAvroDeserializerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
class KafkaAvroDeserializerPropertiesBuilder : KafkaAvroSerDePropertiesBuilder() {
    var specificAvroReader: Boolean? = null
    var specificAvroKeyType: Class<*>? = null
    var specificAvroValueType: Class<*>? = null
    var avroReflectionAllowNull: Boolean? = null
    var avroUseLogicalTypeConverters: Boolean? = null

    override fun doBuild(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()

        specificAvroReader?.let {
            configMap[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = it
        }
        specificAvroKeyType?.let {
            configMap[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_KEY_TYPE_CONFIG] = it
        }
        specificAvroValueType?.let {
            configMap[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE_CONFIG] = it
        }
        avroReflectionAllowNull?.let {
            configMap[KafkaAvroDeserializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG] = it
        }
        avroUseLogicalTypeConverters?.let {
            configMap[KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG] = it
        }

        return configMap
    }
}

/**
 * See [KafkaAvroSerializerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
class KafkaAvroSerializerPropertiesBuilder : KafkaAvroSerDePropertiesBuilder() {
    var avroReflectionAllowNull: Boolean? = null
    var avroUseLogicalTypeConverters: Boolean? = null
    var avroRemoveJavaProperties: Boolean? = null

    override fun doBuild(): KafkaProperties {
        val configMap = mutableMapOf<String, Any?>()

        avroReflectionAllowNull?.let {
            configMap[KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG] = it
        }
        avroUseLogicalTypeConverters?.let {
            configMap[KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG] = it
        }
        avroRemoveJavaProperties?.let {
            configMap[KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG] = it
        }

        return configMap
    }
}
typealias KafkaProperties = MutableMap<String, Any?>

@Suppress("unused")
enum class MessageTimestampType {
    CreateTime,
    LogAppendTime,
}

typealias CompressionType = org.apache.kafka.common.record.CompressionType
