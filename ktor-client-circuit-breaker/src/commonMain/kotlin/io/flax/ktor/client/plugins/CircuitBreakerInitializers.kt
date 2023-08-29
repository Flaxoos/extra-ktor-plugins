package io.flax.ktor.client.plugins

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.util.KtorDsl
import io.ktor.util.collections.ConcurrentMap

/**
 * Register a [CircuitBreaker] with a given name
 */
@CircuitBreakerDsl
fun CircuitBreakerConfig.register(
    name: CircuitBreakerName,
    config: CircuitBreakerConfig.CircuitBreakerBuilder.() -> Unit
) {
    circuitBreakers.addCircuitBreaker(name, config)
}

/**
 * Registers a global [CircuitBreaker] that is applied to the whole client
 */
@CircuitBreakerDsl
fun CircuitBreakerConfig.global(config: CircuitBreakerConfig.CircuitBreakerBuilder.() -> Unit) {
    global = CircuitBreaker(
        CIRCUIT_BREAKER_NAME_GLOBAL,
        CircuitBreakerConfig.CircuitBreakerBuilder().apply(config)
    )
}

/**
 * Apply the [CircuitBreaker] with the given [CircuitBreakerName] to the request or the global one if name is given
 */
fun HttpRequestBuilder.withCircuitBreaker(name: CircuitBreakerName = CIRCUIT_BREAKER_NAME_GLOBAL) {
    setAttributes {
        this.put(CircuitBreakerNameKey, name)
    }
}

/**
 * Make a request with the [CircuitBreaker] with the given [CircuitBreakerName]
 */
suspend fun HttpClient.requestWithCircuitBreaker(
    name: CircuitBreakerName = CIRCUIT_BREAKER_NAME_GLOBAL,
    block: HttpRequestBuilder.() -> Unit
): HttpResponse {
    return request {
        withCircuitBreaker(name)
        block()
    }
}

/**
 * Adds a [CircuitBreaker] to a [ConcurrentMap]
 */
internal fun ConcurrentMap<CircuitBreakerName, CircuitBreaker>.addCircuitBreaker(
    name: CircuitBreakerName,
    config: CircuitBreakerConfig.CircuitBreakerBuilder.() -> Unit
) {
    require(!containsKey(name)) {
        "Circuit Breaker with name $name is already registered"
    }
    put(name, CircuitBreaker(name, CircuitBreakerConfig.CircuitBreakerBuilder().apply(config)))
}
