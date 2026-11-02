package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import io.ktor.client.call.HttpClientCall
import io.ktor.util.logging.KtorSimpleLogger
import kotlin.time.Duration
import arrow.resilience.CircuitBreaker as ArrowCircuitBreaker

private val logger = KtorSimpleLogger("io.github.flaxoos.ktor.client.plugins.CircuitBreaker")

internal class CircuitBreaker(
    private val name: CircuitBreakerName,
    config: CircuitBreakerConfig.CircuitBreakerBuilder,
) {
    private val failureTrigger = config.failureTrigger
    private val onStateChange = config.onStateChange

    private val delegate =
        ArrowCircuitBreaker(
            resetTimeout = config.resetInterval,
            openingStrategy = ArrowCircuitBreaker.OpeningStrategy.Count(maxFailures = config.failureThreshold),
            exponentialBackoffFactor = config.backoffFactor,
            maxResetTimeout = config.maxResetInterval,
            timeSource = config.timeSource,
            onOpen = { changedTo(CircuitBreakerState.OPEN) },
            onHalfOpen = { changedTo(CircuitBreakerState.HALF_OPEN) },
            onClosed = { changedTo(CircuitBreakerState.CLOSED) },
        )

    init {
        @Suppress("DEPRECATION")
        if (config.halfOpenFailureThreshold != DEFAULT_HALF_OPEN_FAILURE_THRESHOLD) {
            logger.warn(
                "Circuit breaker ${name.value} sets halfOpenFailureThreshold, which is not used: " +
                    "an open circuit lets one test call through after resetInterval",
            )
        }
    }

    /**
     * Sends a request through this circuit breaker.
     *
     * @return the call, even when its response counts as a failure.
     * @throws CircuitBreakerException where the circuit is open.
     */
    internal suspend fun protect(send: suspend () -> HttpClientCall): HttpClientCall =
        either {
            catch(
                block = {
                    delegate
                        .protectEither {
                            val call = send()
                            if (call.response.failureTrigger()) throw FailedResponse(call) else call
                        }.bind()
                },
                catch = { failed: FailedResponse -> failed.call },
            )
        }.getOrElse { rejected -> throw CircuitBreakerException(name, rejected.state.retryAfter(), rejected) }

    private suspend fun changedTo(state: CircuitBreakerState) {
        logger.debug("Circuit breaker ${name.value} is $state")
        catch({ onStateChange(name, state) }) { failure ->
            logger.warn("Circuit breaker ${name.value}'s onStateChange failed on $state: $failure")
        }
    }
}

private fun ArrowCircuitBreaker.State.retryAfter(): Duration =
    when (this) {
        is ArrowCircuitBreaker.State.Open -> (-expiresAt.elapsedNow()).coerceAtLeast(Duration.ZERO)
        else -> Duration.ZERO
    }

/** Carries a failed response out of Arrow's circuit breaker, which counts only thrown failures. */
private class FailedResponse(
    val call: HttpClientCall,
) : Exception()

/** Thrown for a request a circuit breaker rejected without sending it. */
class CircuitBreakerException(
    /** The circuit breaker that rejected it. */
    val name: CircuitBreakerName,
    /** How long until it lets a test call through. */
    val retryAfter: Duration,
    /** The circuit breaker's rejection. */
    cause: Throwable? = null,
) : Exception("Circuit breaker ${name.value} is open: it lets a test call through in $retryAfter", cause)
