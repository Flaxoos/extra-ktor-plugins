package io.github.flaxoos.ktor.client.plugins.circuitbreaker

import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerName.Companion.toCircuitBreakerName
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TestCircuitBreakerName = "test".toCircuitBreakerName()
private const val CONCURRENCY_COUNT = 2

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class, ExperimentalKotest::class)
class CircuitBreakerTest : FunSpec() {
    sealed class CircuitBreakerTestCase(
        val name: CircuitBreakerName,
        val failureThreshold: Int,
        val resetInterval: Duration,
        val backoffFactor: Double,
    )

    data object Global : CircuitBreakerTestCase(
        name = CIRCUIT_BREAKER_NAME_GLOBAL,
        failureThreshold = 6,
        resetInterval = 2.seconds,
        backoffFactor = 1.0,
    )

    data object Specific : CircuitBreakerTestCase(
        name = TestCircuitBreakerName,
        failureThreshold = 3,
        resetInterval = 1.seconds,
        backoffFactor = 2.0,
    )

    private var client by Delegates.notNull<HttpClient>()
    private var mockResponse: MockRequestHandleScope.() -> HttpResponseData = { respondOk() }
    private var responseGate: CompletableDeferred<Unit>? = null
    private val stateChanges = mutableListOf<Pair<CircuitBreakerName, CircuitBreakerState>>()

    init {
        context("Circuit Breaker Tests").config(coroutineTestScope = true) {
            context("should let actions through when open") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenOkResponse()

                    shouldNotThrow<Exception> {
                        getWithCircuitBreaker(case.name)
                    }
                }
            }

            context("should switch to open after failure threshold") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()

                    repeat(case.failureThreshold + 1) {
                        getWithCircuitBreaker(case.name) shouldBe InternalServerError
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }
                }
            }

            context("should switch to open after failure threshold of requests that throw") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenTimeout()

                    repeat(case.failureThreshold + 1) {
                        shouldThrow<HttpRequestTimeoutException> {
                            getWithCircuitBreaker(case.name)
                        }
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }
                }
            }

            context("should count only failures in a row, starting again after a success") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()
                    repeat(case.failureThreshold) { getWithCircuitBreaker(case.name) }
                    givenOkResponse()
                    getWithCircuitBreaker(case.name)
                    givenErrorResponse()
                    repeat(case.failureThreshold) { getWithCircuitBreaker(case.name) }

                    getWithCircuitBreaker(case.name) shouldBe InternalServerError
                }
            }

            context("should let a request sent before the circuit opened receive its response") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()

                    repeat(case.failureThreshold) {
                        getWithCircuitBreaker(case.name)
                    }

                    val heldResponses = CompletableDeferred<Unit>()
                    responseGate = heldResponses
                    val inFlight = List(CONCURRENCY_COUNT) { async { getWithCircuitBreaker(case.name) } }
                    testCoroutineScheduler.runCurrent()
                    heldResponses.complete(Unit)

                    inFlight.awaitAll() shouldBe List(CONCURRENCY_COUNT) { InternalServerError }
                }
            }

            context("should state how long until it lets a test call through") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()
                    repeat(case.failureThreshold + 1) { getWithCircuitBreaker(case.name) }

                    shouldThrow<CircuitBreakerException> { getWithCircuitBreaker(case.name) }.retryAfter shouldBe case.resetInterval
                    delay(case.resetInterval / 2)
                    shouldThrow<CircuitBreakerException> { getWithCircuitBreaker(case.name) }.retryAfter shouldBe case.resetInterval / 2
                }
            }

            context("should let one test call through after reset interval elapsed and switch to open again when it fails") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()

                    repeat(case.failureThreshold + 1) {
                        getWithCircuitBreaker(case.name)
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }

                    delay(case.resetInterval)

                    getWithCircuitBreaker(case.name) shouldBe InternalServerError

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }
                }
            }

            context("should reject the other calls while the test call is out") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()
                    repeat(case.failureThreshold + 1) { getWithCircuitBreaker(case.name) }
                    delay(case.resetInterval)
                    givenOkResponse()

                    val heldResponse = CompletableDeferred<Unit>()
                    responseGate = heldResponse
                    val testCall = async { getWithCircuitBreaker(case.name) }
                    testCoroutineScheduler.runCurrent()
                    val rejected = shouldThrow<CircuitBreakerException> { getWithCircuitBreaker(case.name) }
                    heldResponse.complete(Unit)

                    rejected.retryAfter shouldBe Duration.ZERO
                    testCall.await() shouldBe customOk
                }
            }

            context("should wait reset interval times backoff factor after a failed test call") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()
                    repeat(case.failureThreshold + 1) { getWithCircuitBreaker(case.name) }
                    delay(case.resetInterval)
                    getWithCircuitBreaker(case.name)
                    val nextInterval = case.resetInterval * case.backoffFactor

                    delay(nextInterval - 1.milliseconds)
                    shouldThrow<CircuitBreakerException> { getWithCircuitBreaker(case.name) }
                    delay(1.milliseconds)
                    getWithCircuitBreaker(case.name) shouldBe InternalServerError
                }
            }

            context("should switch to closed after failure threshold exceeded and resetInterval period passed and action works again") {
                withData(Global, Specific) { case ->

                    initClient()

                    givenErrorResponse()

                    repeat(case.failureThreshold + 1) {
                        getWithCircuitBreaker(case.name)
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }

                    delay(case.resetInterval)

                    givenOkResponse()

                    shouldNotThrow<Exception> {
                        repeat(case.failureThreshold + 2) {
                            getWithCircuitBreaker(case.name)
                        }
                    }
                }
            }

            context("should report each change of its state") {
                withData(Global, Specific) { case ->
                    initClient()

                    givenErrorResponse()
                    repeat(case.failureThreshold + 1) { getWithCircuitBreaker(case.name) }
                    delay(case.resetInterval)
                    givenOkResponse()
                    getWithCircuitBreaker(case.name)

                    stateChanges shouldBe
                        listOf(
                            case.name to CircuitBreakerState.OPEN,
                            case.name to CircuitBreakerState.HALF_OPEN,
                            case.name to CircuitBreakerState.CLOSED,
                        )
                }
            }
        }
        context("concurrent requests and state transition").config(coroutineTestScope = false) {
            withData(Global, Specific) { case ->

                initClient(useTestCoroutineScheduler = false)

                givenErrorResponse()

                repeat(case.failureThreshold + 1) {
                    getWithCircuitBreaker(case.name)
                }

                fun call(wait: Duration = 0.milliseconds) =
                    async {
                        try {
                            delay(wait)
                            getWithCircuitBreaker(case.name)
                        } catch (e: CircuitBreakerException) {
                            return@async e
                        }
                        null
                    }

                val requests =
                    List(CONCURRENCY_COUNT) {
                        call()
                    }
                val delayedRequests =
                    List(CONCURRENCY_COUNT) {
                        call(case.resetInterval + 100.milliseconds)
                    }

                requests.awaitAll().count { it != null } shouldBe CONCURRENCY_COUNT
                delayedRequests.awaitAll().count { it == null } shouldBe 1
            }
        }
    }

    private fun TestScope.initClient(useTestCoroutineScheduler: Boolean = true) {
        responseGate = null
        stateChanges.clear()
        val mockEngine =
            MockEngine.create {
                if (useTestCoroutineScheduler) {
                    dispatcher = StandardTestDispatcher(testCoroutineScheduler)
                }
                requestHandlers.add { _ ->
                    responseGate?.await()
                    mockResponse()
                }
            }
        client =
            HttpClient(mockEngine) {
                install(CircuitBreaking) {
                    global {
                        failureThreshold = Global.failureThreshold
                        resetInterval = Global.resetInterval
                        backoffFactor = Global.backoffFactor
                        if (useTestCoroutineScheduler) timeSource = testCoroutineScheduler.timeSource
                        onStateChange = { name, state -> stateChanges += name to state }
                        failureTrigger = {
                            status.value >= 400
                        }
                    }
                    register(TestCircuitBreakerName) {
                        failureThreshold = Specific.failureThreshold
                        resetInterval = Specific.resetInterval
                        backoffFactor = Specific.backoffFactor
                        if (useTestCoroutineScheduler) timeSource = testCoroutineScheduler.timeSource
                        onStateChange = { name, state -> stateChanges += name to state }
                        failureTrigger = {
                            status.value >= 400
                        }
                    }
                }
            }
    }

    private fun givenErrorResponse() {
        mockResponse = {
            respondError(InternalServerError)
        }
    }

    private fun givenTimeout() {
        mockResponse = {
            throw HttpRequestTimeoutException(url = "http://localhost/", timeoutMillis = 1)
        }
    }

    private fun givenOkResponse() {
        mockResponse = {
            respond("Ok-ish", customOk)
        }
    }

    private val customOk = HttpStatusCode(300, "It's ok!")

    private suspend fun getWithCircuitBreaker(name: CircuitBreakerName) = client.requestWithCircuitBreaker(name = name) {}.status
}
