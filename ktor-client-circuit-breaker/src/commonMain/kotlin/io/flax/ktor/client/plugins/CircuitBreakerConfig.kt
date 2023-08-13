package io.flax.ktor.client.plugins

import io.flax.ktor.client.plugins.CircuitBreakerState.HALF_OPEN
import io.flax.ktor.client.plugins.CircuitBreakerState.OPEN
import io.ktor.util.KtorDsl
import io.ktor.util.collections.ConcurrentMap
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for [CircuitBreaker].
 */
@KtorDsl
class CircuitBreakerConfig {
    internal val circuitBreakers: ConcurrentMap<CircuitBreakerName, CircuitBreaker> = ConcurrentMap()
    internal var global: CircuitBreaker? = null

    class CircuitBreakerBuilder {
        /**
         * How many failures are to be tolerated before the circuit moves to [HALF_OPEN].
         */
        var failureThreshold = 3

        /**
         * How many attempts are allowed in [HALF_OPEN] state.
         */
        var halfOpenFailureThreshold = 2

        /**
         * How long to wait before moving from [OPEN] to [HALF_OPEN]
         */
        var resetInterval: Duration = 1.seconds
    }
}

/**
 * Value class for a [CircuitBreaker] name
 */
@JvmInline
value class CircuitBreakerName(val value: String) {
    companion object {
        fun String.toCircuitBreakerName() = CircuitBreakerName(this)
    }
}