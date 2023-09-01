package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerState.HALF_OPEN
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerState.OPEN
import io.ktor.client.statement.HttpResponse
import io.ktor.util.collections.ConcurrentMap
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@DslMarker
annotation class CircuitBreakerDsl

/**
 * Configuration for [CircuitBreaker].
 */

@CircuitBreakerDsl
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
         * How long to wait before moving from [OPEN] to [HALF_OPEN].
         */
        var resetInterval: Duration = 1.seconds

        /**
         * What is considered a failure. default is [HttpResponse.status] >= 300
         */
        var failureTrigger: HttpResponse.() -> Boolean = {
            status.value >= 300
        }
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
