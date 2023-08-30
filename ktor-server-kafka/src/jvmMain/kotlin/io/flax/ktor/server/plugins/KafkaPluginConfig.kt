package io.flax.ktor.server.plugins

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.flax.ktor.server.plugins.Defaults.DEFAULT_CLIENT_ID
import io.flax.ktor.server.plugins.Defaults.DEFAULT_CONSUMER_POLL_FREQUENCY_MS
import io.flax.ktor.server.plugins.Defaults.DEFAULT_GROUP_ID
import io.flax.ktor.server.plugins.Defaults.DEFAULT_TOPIC_PARTITIONS
import io.flax.ktor.server.plugins.Defaults.DEFAULT_TOPIC_REPLICAS
import io.ktor.server.config.ApplicationConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@DslMarker
annotation class KafkaDsl

@KafkaDsl
sealed class AbstractKafkaConfig {
    abstract val schemaRegistryUrl: List<String>?
    internal abstract val commonProperties: KafkaPropertyStore?
    internal abstract val adminProperties: KafkaDelegatingPropertyStore?
    internal abstract val producerProperties: KafkaDelegatingPropertyStore?
    internal abstract val consumerProperties: KafkaDelegatingPropertyStore?
    internal abstract val topics: List<NewTopic>

    /**
     * Because the consumer is operating in the background, it can be defined in the setup phase
     */
    var consumerConfig: KafkaConsumerConfig? = null
}

class KafkaConsumerConfig {
    var consumerRecordHandlers: MutableMap<TopicName, ConsumerRecordHandler> = mutableMapOf()
    var consumerPollFrequency: Duration = DEFAULT_CONSUMER_POLL_FREQUENCY_MS.milliseconds
}

class KafkaConfig : AbstractKafkaConfig() {
    override val topics: List<NewTopic> by lazy {
        topicBuilders.map { it.build() }
    }
    override var schemaRegistryUrl: List<String>? = emptyList()

    override val commonProperties: KafkaPropertyStore? by lazy {
        commonPropertiesBuilder?.build()
    }
    override val adminProperties: KafkaDelegatingPropertyStore? by lazy {
        adminPropertiesBuilder?.build()?.withDefaultAdminConfig()
    }
    override val producerProperties: KafkaDelegatingPropertyStore? by lazy {
        producerPropertiesBuilder?.build()?.withDefaultProducerConfig()
    }
    override val consumerProperties: KafkaDelegatingPropertyStore? by lazy {
        consumerPropertiesBuilder?.build()?.withDefaultConsumerConfig()
    }

    internal val topicBuilders = mutableListOf<TopicBuilder>()
    internal var commonPropertiesBuilder: CommonClientPropertiesBuilder? = null
    internal var adminPropertiesBuilder: AdminPropertiesBuilder? = null
    internal var producerPropertiesBuilder: ProducerPropertiesBuilder? = null
    internal var consumerPropertiesBuilder: ConsumerPropertiesBuilder? = null
}

class KafkaFileConfig(config: ApplicationConfig) : AbstractKafkaConfig() {
    override var schemaRegistryUrl: List<String> =
        config.propertyOrNull("schema.registry.url")?.getList() ?: emptyList()

    override val commonProperties: KafkaDelegatingPropertyStore =
        KafkaDelegatingPropertyStore(config.config("common").toMap().toMutableMap(), null)
    override val adminProperties: KafkaDelegatingPropertyStore =
        KafkaDelegatingPropertyStore(config.config("admin").toMap().toMutableMap(), commonProperties.store)
    override val producerProperties: KafkaDelegatingPropertyStore =
        KafkaDelegatingPropertyStore(config.config("producer").toMap().toMutableMap(), commonProperties.store)
    override val consumerProperties: KafkaDelegatingPropertyStore =
        KafkaDelegatingPropertyStore(config.config("consumer").toMap().toMutableMap(), commonProperties.store)

    override val topics: List<NewTopic> = config.configList("topics").map { TopicBuilder.froMap(it.toMap()).build() }

}

context (AbstractKafkaConfig)
internal fun KafkaDelegatingPropertyStore.withDefaultAdminConfig() = apply {
    getOrPut(CommonClientConfigs.CLIENT_ID_CONFIG) { DEFAULT_CLIENT_ID }

}

context (AbstractKafkaConfig)
internal fun KafkaDelegatingPropertyStore.withDefaultProducerConfig() = apply {
    getOrPut(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG) { schemaRegistryUrl }
    getOrPut(ProducerConfig.CLIENT_ID_CONFIG) { DEFAULT_CLIENT_ID }
    getOrPut(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG) { StringSerializer::class.java.name }
    getOrPut(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG) { KafkaAvroSerializer::class.java.name }
}

context (AbstractKafkaConfig)
internal fun KafkaDelegatingPropertyStore.withDefaultConsumerConfig() = apply {
    getOrPut(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG) { schemaRegistryUrl }
    getOrPut(ConsumerConfig.GROUP_ID_CONFIG) { DEFAULT_GROUP_ID }
    getOrPut(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG) { StringDeserializer::class.java.name }
    getOrPut(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG) { KafkaAvroDeserializer::class.java.name }
}

@KafkaDsl
fun KafkaConsumerConfig.consumerRecordHandler(topicName: TopicName, handler: ConsumerRecordHandler) {
    consumerRecordHandlers[topicName] = handler
}


@KafkaDsl
fun KafkaConfig.common(configuration: CommonClientPropertiesBuilder.() -> Unit = { CommonClientPropertiesBuilder }) {
    commonPropertiesBuilder =
        CommonClientPropertiesBuilder.apply(configuration)
}

@KafkaDsl
fun KafkaConfig.admin(configuration: AdminPropertiesBuilder.() -> Unit = { AdminPropertiesBuilder(delegate) }) {
    adminPropertiesBuilder = AdminPropertiesBuilder(commonPropertiesBuilder).apply(configuration)
}

@KafkaDsl
fun KafkaConfig.topic(name: TopicName, block: TopicBuilder.() -> Unit) {
    topicBuilders.add(TopicBuilder(name).apply(block))
}

@KafkaDsl
fun KafkaConfig.producer(
    configuration: ProducerPropertiesBuilder.() -> Unit = { ProducerPropertiesBuilder(schemaRegistryUrl, delegate) }
) {
    producerPropertiesBuilder =
        ProducerPropertiesBuilder(
            //TODO: assuming only avro is used, support custom serializers later
            with(checkNotNull(schemaRegistryUrl) { "Consumer schema registry url is not set" }) {
                check(isNotEmpty()) { "Schema registry url is not set" }
                this
            },
            commonPropertiesBuilder
        ).apply(configuration)
}

@KafkaDsl
fun KafkaConfig.consumer(
    configuration: ConsumerPropertiesBuilder.() -> Unit = { ConsumerPropertiesBuilder(schemaRegistryUrl, delegate) }
) {
    consumerPropertiesBuilder =
        ConsumerPropertiesBuilder(
            //TODO: assuming only avro is used, support custom serializers later
            with(checkNotNull(schemaRegistryUrl) { "Consumer schema registry url is not set" }) {
                check(isNotEmpty()) { "Schema registry url is not set" }
                this
            }, commonPropertiesBuilder
        ).apply(configuration)
}

@KafkaDsl
fun AbstractKafkaConfig.consumerConfig(
    configuration: KafkaConsumerConfig.() -> Unit = { }
) {
    consumerConfig = KafkaConsumerConfig().apply(configuration)
}

@Suppress("MemberVisibilityCanBePrivate")
class TopicBuilder(internal val name: TopicName) {
    var partitions: Int = DEFAULT_TOPIC_PARTITIONS
    var replicas: Short = DEFAULT_TOPIC_REPLICAS
    var replicasAssignments: Map<Int, List<Int?>>? = null
    internal var configs: Map<String, Any?>? = null

    @KafkaDsl
    fun configs(config: TopicPropertiesBuilder.() -> Unit) {
        configs = TopicPropertiesBuilder().apply(config).build().store
    }

    internal fun build(): NewTopic {
        val topic = if (replicasAssignments == null) {
            NewTopic(name.value, partitions, replicas)
        } else {
            NewTopic(name.value, replicasAssignments)
        }
        return topic.configs(configs?.filterValues { it != null }?.mapValues { it.value.toString() })
    }

    companion object {
        fun froMap(map: Map<String, Any?>): TopicBuilder {
            return TopicBuilder(TopicName.named(map["name"] as String)).apply {
                partitions = map["partitions"] as Int
                replicas = (map["replicas"] as Int).toShort()
                replicasAssignments = map["replicasAssignments"] as Map<Int, List<Int?>>?
                configs = map["configs"] as Map<String, Any?>?
            }
        }
    }
}

sealed interface KafkaPropertiesBuilder {
    fun build(): KafkaDelegatingPropertyStore
}

/**
 * See [TopicConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
class TopicPropertiesBuilder : KafkaPropertiesBuilder {
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
    var messageFormatVersion: String? = null
    var messageTimestampType: MessageTimestampType? = null
    var messageTimestampDifferenceMaxMs: Long? = null
    var messageDownconversionEnable: Boolean? = null

    override fun build(): KafkaDelegatingPropertyStore {
        val configMap = mutableMapOf<String, Any?>()

        segmentBytes?.let { configMap["segment.bytes"] = it }
        segmentMs?.let { configMap["segment.ms"] = it }
        segmentJitterMs?.let { configMap["segment.jitter.ms"] = it }
        segmentIndexBytes?.let { configMap["segment.index.bytes"] = it }
        flushMessagesInterval?.let { configMap["flush.messages"] = it }
        flushMs?.let { configMap["flush.ms"] = it }
        retentionBytes?.let { configMap["retention.bytes"] = it }
        retentionMs?.let { configMap["retention.ms"] = it }
        maxMessageBytes?.let { configMap["max.message.bytes"] = it }
        indexIntervalBytes?.let { configMap["index.interval.bytes"] = it }
        fileDeleteDelayMs?.let { configMap["file.delete.delay.ms"] = it }
        deleteRetentionMs?.let { configMap["delete.retention.ms"] = it }
        minCompactionLagMs?.let { configMap["min.compaction.lag.ms"] = it }
        maxCompactionLagMs?.let { configMap["max.compaction.lag.ms"] = it }
        minCleanableDirtyRatio?.let { configMap["min.cleanable.dirty.ratio"] = it }
        cleanupPolicy?.let { configMap["cleanup.policy"] = it }
        uncleanLeaderElectionEnable?.let { configMap["unclean.leader.election.enable"] = it }
        minInSyncReplicas?.let { configMap["min.insync.replicas"] = it }
        compressionType?.let { configMap["compression.type"] = it }
        preallocate?.let { configMap["preallocate"] = it }
        messageFormatVersion?.let { configMap["message.format.version"] = it }
        messageTimestampType?.let { configMap["message.timestamp.type"] = it }
        messageTimestampDifferenceMaxMs?.let { configMap["message.timestamp.difference.max.ms"] = it }
        messageDownconversionEnable?.let { configMap["message.downconversion.enable"] = it }

        return KafkaDelegatingPropertyStore(configMap)
    }
}

/**
 * see [CommonClientConfigs]
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class ClientPropertiesBuilder : KafkaPropertiesBuilder {
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


    fun buildCommon(): KafkaDelegatingPropertyStore {
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
        return KafkaDelegatingPropertyStore(configMap)
    }

    override fun build() = buildCommon()
}

data object CommonClientPropertiesBuilder : ClientPropertiesBuilder()

interface Delegating {
    val delegate: CommonClientPropertiesBuilder?
}

class AdminPropertiesBuilder(override val delegate: CommonClientPropertiesBuilder?) :
    ClientPropertiesBuilder(), Delegating {
    override fun build(): KafkaDelegatingPropertyStore {
        return KafkaDelegatingPropertyStore(super.build().store, delegate?.build()?.store)
    }
}

interface SchemaRegistryProvider {
    abstract var schemaRegistryUrl: List<String>?
}

/**
 * see [ProducerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
class ProducerPropertiesBuilder(
    override var schemaRegistryUrl: List<String>? = null,
    override val delegate: CommonClientPropertiesBuilder?
) : ClientPropertiesBuilder(), Delegating, SchemaRegistryProvider {
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

    override fun build(): KafkaDelegatingPropertyStore {
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

        return KafkaDelegatingPropertyStore(configMap.store, delegate?.build()?.store)
    }
}

/**
 * see [ConsumerConfig]
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConsumerPropertiesBuilder(
    override var schemaRegistryUrl: List<String>? = null,
    override val delegate: CommonClientPropertiesBuilder?
) : ClientPropertiesBuilder(), Delegating, SchemaRegistryProvider {
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

    override fun build(): KafkaDelegatingPropertyStore {
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

        return KafkaDelegatingPropertyStore(configMap.store, delegate?.build()?.store)
    }
}

sealed class KafkaPropertyStore(
    open val store: MutableMap<String, Any?>
) : Map<String, Any?> by store

class KafkaDelegatingPropertyStore(
    override val store: MutableMap<String, Any?>,
    var delegate: MutableMap<String, Any?>? = null
) : KafkaPropertyStore(store) {
    override operator fun get(key: String): Any? {
        return store[key] ?: delegate?.get(key)
    }

    operator fun set(key: String, value: Any?) {
        store[key] = value
    }

    fun getOrPut(key: String, defaultValue: () -> Any?) {
        store.getOrPut(key, defaultValue)
    }
}


@Suppress("unused")
enum class MessageTimestampType {
    CreateTime, LogAppendTime
}

typealias CompressionType = org.apache.kafka.common.record.CompressionType