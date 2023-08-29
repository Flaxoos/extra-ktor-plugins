package io.flax.ktor.server.plugins.components

import io.ktor.server.config.ApplicationConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes.serdeFrom
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Produced
import java.util.Properties

fun streams(
    t: Topology,
    config: ApplicationConfig,
): KafkaStreams {
    val p: Properties = effectiveStreamProperties(config)
    return KafkaStreams(t, p)
}

fun effectiveStreamProperties(config: ApplicationConfig): Properties {
    val bootstrapServers: List<String> = config.property("ktor.kafka.bootstrap.servers").getList()

    // common config
    val commonConfig = config.config("ktor.kafka.properties").toMap()
    // kafka streams
    val streamsConfig = config.config("ktor.kafka.streams").toMap()

    return Properties().apply {
        putAll(commonConfig)
        putAll(streamsConfig)
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }
}

inline fun <reified K, reified V> producedWith(): Produced<K, V> =
    Produced.with(serdeFrom(K::class.java), serdeFrom(V::class.java))
