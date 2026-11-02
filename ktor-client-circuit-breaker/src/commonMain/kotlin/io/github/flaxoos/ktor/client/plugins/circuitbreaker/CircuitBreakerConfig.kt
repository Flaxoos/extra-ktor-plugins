package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import io.ktor.client.statement.HttpResponse
import io.ktor.util.collections.ConcurrentMap
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val DEFAULT_FAILURE_THRESHOLD = 3
internal const val DEFAULT_HALF_OPEN_FAILURE_THRESHOLD = 2
private const val DEFAULT_BACKOFF_FACTOR = 1.0
private const val LOWEST_FAILING_STATUS = 300
private val DEFAULT_RESET_INTERVAL = 1.seconds

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
        /** How many failures in a row are tolerated before the next one opens the circuit. */
        var failureThreshold = DEFAULT_FAILURE_THRESHOLD

        /** Not used. */
        @Deprecated("Not used: an open circuit lets one test call through after resetInterval.")
        var halfOpenFailureThreshold = DEFAULT_HALF_OPEN_FAILURE_THRESHOLD

        /** How long an open circuit rejects calls before it lets one test call through. */
        var resetInterval: Duration = DEFAULT_RESET_INTERVAL

        /** What each failed test call multiplies [resetInterval] by. */
        var backoffFactor: Double = DEFAULT_BACKOFF_FACTOR

        /** The longest [resetInterval] grows to. */
        var maxResetInterval: Duration = Duration.INFINITE

        /** Where the circuit breaker reads the time from. */
        var timeSource: TimeSource = TimeSource.Monotonic

        /**
         * What is considered a failure. default is [HttpResponse.status] >= 300
         */
        var failureTrigger: HttpResponse.() -> Boolean = {
            status.value >= LOWEST_FAILING_STATUS
        }

        /**
         * Called with each change of the circuit breaker's state.
         *
         * It runs inside the request that changed the state, so it should be quick.
         */
        var onStateChange: suspend (CircuitBreakerName, CircuitBreakerState) -> Unit = { _, _ -> }
    }
}

/** The state of a circuit breaker. */
enum class CircuitBreakerState {
    /** Lets every request through. */
    CLOSED,

    /** Rejects every request. */
    OPEN,

    /** Lets one test request through, and rejects the rest until it is answered. */
    HALF_OPEN,
}

/**
 * Value class for a [CircuitBreaker] name
 */
@JvmInline
value class CircuitBreakerName(
    val value: String,
) {
    companion object {
        fun String.toCircuitBreakerName() = CircuitBreakerName(this)
    }
}
