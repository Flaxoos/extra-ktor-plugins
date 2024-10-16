package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.ClientPluginBuilder
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.util.AttributeKey
import io.ktor.util.collections.ConcurrentMap

internal val CIRCUIT_BREAKER_NAME_GLOBAL = CircuitBreakerName("KTOR_GLOBAL_RATE_LIMITER")

internal val CircuitBreakerInstancesRegistryKey =
    AttributeKey<ConcurrentMap<CircuitBreakerName, CircuitBreaker>>("CircuitBreakerInstancesRegistryKey")
internal val CircuitBreakerNameKey =
    AttributeKey<CircuitBreakerName>("CircuitBreakerInstancesRegistryKey")

val CircuitBreaking =
    createClientPlugin("CircuitBreaker", ::CircuitBreakerConfig) {
        val global = pluginConfig.global
        val instances =
            pluginConfig.circuitBreakers.apply {
                if (global != null) {
                    put(CIRCUIT_BREAKER_NAME_GLOBAL, global)
                }
            }
        require(instances.isNotEmpty()) { "At least one circuit breaker must be specified" }
        client.circuitBreakerRegistry().putAll(
            instances.mapValues { entry ->
                entry.value.apply { initialize(client.engine.dispatcher) }
            },
        )
        circuitBreakerPluginBuilder()
    }

internal fun HttpClient.circuitBreakerRegistry() = this.attributes.computeIfAbsent(CircuitBreakerInstancesRegistryKey) { ConcurrentMap() }

internal fun ClientPluginBuilder<CircuitBreakerConfig>.circuitBreakerPluginBuilder() {
    val instanceRegistry = client.circuitBreakerRegistry()

    onRequest { request, _ ->
        request.attributes.getOrNull(CircuitBreakerNameKey)?.let { circuitBreakerName ->
            instanceRegistry.getValue(circuitBreakerName).wire()
        }
    }

    onResponse { response ->
        response.request.attributes.getOrNull(CircuitBreakerNameKey)?.let { circuitBreakerName ->
            instanceRegistry.getValue(circuitBreakerName).handleResponse(response)
        }
    }
}
