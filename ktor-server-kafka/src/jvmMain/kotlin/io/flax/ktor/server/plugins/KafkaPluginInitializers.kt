package io.flax.ktor.server.plugins

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@DslMarker
annotation class KafkaDsl

fun Application.installKafka(
    configurationPath: String = DEFAULT_CONFIG_PATH,
    config: KafkaPathConfig.() -> Unit
) {
    install(kafkaFromConfig(configurationPath, config))
}

fun Application.installKafkaWith(config: KafkaConfig.() -> Unit) {
    install(Kafka) { config() }
}

@Suppress("MemberVisibilityCanBePrivate")
class TopicBuilder(internal val topicName: TopicName) {
    var partitions = 1
    var replicas: Short = 1
    var replicasAssignments: Map<Int, List<Int?>>? = null
    var configs: Map<String, String>? = null

    internal fun build() = (if (replicasAssignments == null)
        NewTopic(topicName.name, partitions, replicas)
    else
        NewTopic(topicName.name, replicasAssignments)).configs(configs)
}

@KafkaDsl
sealed class AbstractKafkaConfig {
    internal abstract val bootstrapServers: List<String>
    internal abstract var schemaRegistryUrl: List<String>

    internal abstract val adminProperties: KafkaProperties?
    internal abstract val producerProperties: KafkaProperties?
    internal abstract val consumerProperties: KafkaProperties?

    internal val topicBuilders = mutableListOf<TopicBuilder>()
    internal val consumerRecordHandlers: MutableMap<TopicName, ConsumerRecordHandler> = mutableMapOf()
    internal val consumerPollFrequency: Duration = DEFAULT_CONSUMER_POLL_FREQUENCY_MS.milliseconds
    internal val consumerOperations: MutableList<ConsumerFlow.(Application) -> ConsumerFlow> = mutableListOf()
}

class KafkaConfig : AbstractKafkaConfig() {
    public override var bootstrapServers: List<String> = emptyList()
    public override var schemaRegistryUrl: List<String> = emptyList()

    internal var adminPropertiesBuilder: AdminPropertiesBuilder? = null
    internal var producerPropertiesBuilder: ProducerPropertiesBuilder? = null
    internal var consumerPropertiesBuilder: ConsumerPropertiesBuilder? = null

    override val adminProperties: KafkaProperties? =
        adminPropertiesBuilder?.build()?.withDefaultAdminConfig()
    override val producerProperties: KafkaProperties? =
        producerPropertiesBuilder?.build()?.withDefaultProducerConfig()
    override val consumerProperties: KafkaProperties? =
        consumerPropertiesBuilder?.build()?.withDefaultConsumerConfig()
}

class KafkaPathConfig(config: ApplicationConfig) : AbstractKafkaConfig() {
    override val bootstrapServers = config.property("bootstrap.servers").getList()
    override var schemaRegistryUrl: List<String> =
        config.propertyOrNull("properties.schema.registry.url")?.getList() ?: emptyList()
    override val adminProperties: KafkaProperties =
        config.config("properties").toMap().toMutableMap().withDefaultAdminConfig()
    override val producerProperties: KafkaProperties =
        config.config("producer").toMap().toMutableMap().withDefaultProducerConfig()
    override val consumerProperties: KafkaProperties =
        config.config("consumer").toMap().toMutableMap().withDefaultConsumerConfig()

}

context (AbstractKafkaConfig)
internal fun KafkaProperties.withDefaultAdminConfig() = apply {
    putBootstrapServers()
}

context (AbstractKafkaConfig)
internal fun KafkaProperties.withDefaultProducerConfig() = apply {
    putBootstrapServers()
    getOrPut(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG) { schemaRegistryUrl }
    getOrPut(ProducerConfig.CLIENT_ID_CONFIG) { DEFAULT_CLIENT_ID }
    getOrPut(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG) { StringSerializer::class.java.name }
    getOrPut(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG) { KafkaAvroSerializer::class.java.name }
}

context (AbstractKafkaConfig)
internal fun KafkaProperties.withDefaultConsumerConfig() = apply {
    putBootstrapServers()
    getOrPut(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG) { schemaRegistryUrl }
    getOrPut(ConsumerConfig.GROUP_ID_CONFIG) { DEFAULT_GROUP_ID }
    getOrPut(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG) { StringDeserializer::class.java.name }
    getOrPut(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG) { KafkaAvroDeserializer::class.java.name }
}

context (AbstractKafkaConfig)
private fun KafkaProperties.putBootstrapServers() {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
}

@KafkaDsl
fun AbstractKafkaConfig.topic(name: TopicName, block: TopicBuilder.() -> Unit) {
    topicBuilders.add(TopicBuilder(name).apply(block))
}

@KafkaDsl
fun AbstractKafkaConfig.consumerRecordHandler(topicName: TopicName, handler: ConsumerRecordHandler) {
    consumerRecordHandlers[topicName] = handler
}

@KafkaDsl
fun KafkaConfig.admin(configuration: AdminPropertiesBuilder.() -> Unit = { AdminPropertiesBuilder() }) {
    adminPropertiesBuilder = AdminPropertiesBuilder().apply(configuration)
}

@KafkaDsl
fun KafkaConfig.producer(
    configuration: ProducerPropertiesBuilder.() -> Unit = { ProducerPropertiesBuilder(schemaRegistryUrl) }
) {
    producerPropertiesBuilder =
        ProducerPropertiesBuilder(schemaRegistryUrl.also { check(it.isNotEmpty()) { "Schema registry url is not set" } }).apply(
            configuration
        )
}

@KafkaDsl
fun KafkaConfig.consumer(
    configuration: ConsumerPropertiesBuilder.() -> Unit = { ConsumerPropertiesBuilder(schemaRegistryUrl) }
) {
    consumerPropertiesBuilder =
        ConsumerPropertiesBuilder(schemaRegistryUrl.also { check(it.isNotEmpty()) { "Schema registry url is not set" } }).apply(
            configuration
        )
}

@Suppress("MemberVisibilityCanBePrivate")
abstract class CommonClientPropertiesBuilder {
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

    protected fun buildCommon(): KafkaProperties {
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

    abstract fun build(): KafkaProperties
}

private interface SchemaRegistryDependant {
    val schemaRegistryUrl: List<String>
}

/**
 * see [CommonClientConfigs]
 */
class AdminPropertiesBuilder : CommonClientPropertiesBuilder() {
    override fun build() = buildCommon()
}

/**
 * see [ProducerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
class ProducerPropertiesBuilder(override val schemaRegistryUrl: List<String>) : CommonClientPropertiesBuilder(),
    SchemaRegistryDependant {
    var batchSize: Any? = null
    var acks: Any? = null
    var lingerMs: Any? = null
    var deliveryTimeoutMs: Any? = null
    var maxRequestSize: Any? = null
    var maxBlockMs: Any? = null
    var bufferMemory: Any? = null
    var compressionType: Any? = null
    var maxInFlightRequestsPerConnection: Any? = null
    var keySerializerClass: Any? = null
    var valueSerializerClass: Any? = null
    var partitionerClass: Any? = null
    var interceptorClasses: Any? = null
    var enableIdempotence: Any? = null
    var transactionTimeout: Any? = null
    var transactionalId: Any? = null

    override fun build(): KafkaProperties {
        val configMap = buildCommon()
        batchSize?.let { configMap[ProducerConfig.BATCH_SIZE_CONFIG] = it }
        acks?.let { configMap[ProducerConfig.ACKS_CONFIG] = it }
        lingerMs?.let { configMap[ProducerConfig.LINGER_MS_CONFIG] = it }
        deliveryTimeoutMs?.let { configMap[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = it }
        maxRequestSize?.let { configMap[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = it }
        maxBlockMs?.let { configMap[ProducerConfig.MAX_BLOCK_MS_CONFIG] = it }
        bufferMemory?.let { configMap[ProducerConfig.BUFFER_MEMORY_CONFIG] = it }
        compressionType?.let { configMap[ProducerConfig.COMPRESSION_TYPE_CONFIG] = it }
        maxInFlightRequestsPerConnection?.let { configMap[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = it }
        keySerializerClass?.let { configMap[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = it }
        valueSerializerClass?.let { configMap[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = it }
        partitionerClass?.let { configMap[ProducerConfig.PARTITIONER_CLASS_CONFIG] = it }
        interceptorClasses?.let { configMap[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = it }
        enableIdempotence?.let { configMap[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = it }
        transactionTimeout?.let { configMap[ProducerConfig.TRANSACTION_TIMEOUT_CONFIG] = it }
        transactionalId?.let { configMap[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = it }

        return configMap
    }
}

/**
 * see [ConsumerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConsumerPropertiesBuilder(override val schemaRegistryUrl: List<String>) : CommonClientPropertiesBuilder(),
    SchemaRegistryDependant {
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

    override fun build(): KafkaProperties {
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
    }
}

typealias KafkaProperties = MutableMap<String, Any?>