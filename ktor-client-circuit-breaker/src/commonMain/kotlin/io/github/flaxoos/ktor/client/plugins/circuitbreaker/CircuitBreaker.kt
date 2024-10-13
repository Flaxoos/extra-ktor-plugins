package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerState.CLOSED
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerState.HALF_OPEN
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerState.OPEN
import io.ktor.client.statement.HttpResponse
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.time.Duration

private val logger = KtorSimpleLogger("io.github.flaxoos.ktor.client.plugins.CircuitBreaker")

internal class CircuitBreaker(
    private val name: CircuitBreakerName,
    config: CircuitBreakerConfig.CircuitBreakerBuilder,
) {
    private val failureThreshold: Int = config.failureThreshold
    private val halfOpenFailureThreshold: Int = config.halfOpenFailureThreshold
    private val resetInterval: Duration = config.resetInterval
    private val failureTrigger = config.failureTrigger
    private val failureCounter = atomic(0)
    private val _state = atomic(CLOSED)
    private val responseDecorator: HttpResponse.() -> Unit = {}

    private var scope: CoroutineScope by Delegates.notNull()

    // TODO: Add State Change Events:
    // It might be useful to have listeners or callbacks to notify external components when the state changes,
    // especially for logging or analytics purposes.

    internal fun initialize(dispatcher: CoroutineDispatcher) {
        scope = CoroutineScope(dispatcher)
    }

    internal fun wire() {
        logger.trace("Wired to a request")
        with(_state.value) {
            when (this) {
                CLOSED -> {
                    logger.debug("Circuit breaker is closed")
                    return
                }

                HALF_OPEN -> {
                    logger.debug("Circuit breaker is half open")
                    return
                }

                OPEN -> {
                    logger.debug("Circuit breaker is open, Throwing CircuitBreakerException")
                    throw CircuitBreakerException(failureThreshold)
                }
            }
        }
    }

    internal fun handleResponse(response: HttpResponse) {
        logger.trace("Handling response status ${response.status.value}")
        with(_state.value) {
            when (this) {
                CLOSED -> handleResponse(this, response)
                HALF_OPEN -> handleResponse(this, response)
                OPEN -> error("Circuit breaker is already open")
            }
        }
        response.responseDecorator()
        logger.trace("Finished handling response status ${response.status.value}")
    }

    private fun handleResponse(state: CircuitBreakerState, response: HttpResponse) {
        val selectedFailureThreshold = when (state) {
            CLOSED -> failureThreshold
            HALF_OPEN -> halfOpenFailureThreshold
            OPEN -> error("Circuit breaker is already open")
        }
        val failureCount = failureCounter.value
        if (failureCount < selectedFailureThreshold) {
            if (!response.failureTrigger()) {
                closeCircuit()
                return
            } else {
                failureCounter.updateAndGet { it + 1 }.also { logger.debug("Incremented error counter to $it") }
            }
        } else {
            logger.debug("failure count of $failureCount exceeded threshold of $selectedFailureThreshold for state $state, opening circuit")
            openCircuit()
        }
    }

    private fun openCircuit() {
        logger.trace("changing to open for $resetInterval before changing back to half-open")
        _state.update { OPEN }
        scope.launch(CoroutineName("CircuitBreaker-$name-half-opener")) {
            logger.debug("will switch to half open in ${resetInterval.inWholeMilliseconds} ms")
            delay(resetInterval)
            halfOpenCircuit()
        }
        logger.debug("changed to open")
    }

    private fun halfOpenCircuit() {
        logger.trace("changing to half open")
        failureCounter.update { 0 }
        _state.update { HALF_OPEN }
        logger.debug("changed to half open")
    }

    private fun closeCircuit() {
        logger.trace("changing to closed, setting error counter to 0")
        failureCounter.update { 0 }
        _state.update { CLOSED }
        logger.debug("changed to closed, set error counter to 0")
    }
}

internal enum class CircuitBreakerState {
    CLOSED, OPEN, HALF_OPEN
}

class CircuitBreakerException(failureThreshold: Int) :
    Exception("Action failed more than $failureThreshold times, subsequent calls will be prevented until action is successful again")
