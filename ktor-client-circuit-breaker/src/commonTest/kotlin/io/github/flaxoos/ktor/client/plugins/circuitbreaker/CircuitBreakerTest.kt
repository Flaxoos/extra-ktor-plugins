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
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
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
        val halfOpenFailureThreshold: Int,
        val resetInterval: Duration
    )

    data object Global : CircuitBreakerTestCase(
        name = CIRCUIT_BREAKER_NAME_GLOBAL,
        failureThreshold = 6,
        halfOpenFailureThreshold = 4,
        resetInterval = 2.seconds
    )

    data object Specific : CircuitBreakerTestCase(
        name = TestCircuitBreakerName,
        failureThreshold = 3,
        halfOpenFailureThreshold = 2,
        resetInterval = 1.seconds
    )

    private var client by Delegates.notNull<HttpClient>()
    private var mockResponse: MockRequestHandleScope.() -> HttpResponseData = { respondOk() }

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

            context("should switch to half open after reset interval elapsed and switch to open again after half open attempts exceeded") {
                withData(Global, Specific) { case ->

                    initClient()

                    givenErrorResponse()

                    repeat(case.failureThreshold + 1) {
                        getWithCircuitBreaker(case.name)
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }

                    testCoroutineScheduler.advanceUntilIdle()

                    repeat(case.halfOpenFailureThreshold + 1) {
                        getWithCircuitBreaker(case.name)
                    }

                    shouldThrow<CircuitBreakerException> {
                        getWithCircuitBreaker(case.name)
                    }
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

                    testCoroutineScheduler.advanceUntilIdle()

                    givenOkResponse()

                    shouldNotThrow<Exception> {
                        repeat(case.failureThreshold + 2) {
                            getWithCircuitBreaker(case.name)
                        }
                    }
                }
            }
        }
        // TODO: this is flaky in concurrency counts > 1 or even 2, which suggests the concurrency setup is wrong.
        // getting: IllegalStateException: Circuit breaker is already open
        // This is interesting. It maybe due to the default dispatcher used by the mock engine, though default should
        // have "The maximum number of threads ... is equal to the number of CPU cores, but is at least two."
        // Need to try with an explicitly bigger dispatcher
        xcontext("concurrent requests and state transition").config(coroutineTestScope = false) {
            withData(Global, Specific) { case ->

                initClient(useTestCoroutineScheduler = false)

                givenErrorResponse()

                repeat(case.failureThreshold + 1) {
                    getWithCircuitBreaker(case.name)
                }
                fun call(wait: Duration = 0.milliseconds) = async {
                    try {
                        delay(wait)
                        getWithCircuitBreaker(case.name)
                    } catch (e: CircuitBreakerException) {
                        return@async e
                    }
                    null
                }

                val requests = List(CONCURRENCY_COUNT) {
                    call()
                }
                val delayedRequests = List(CONCURRENCY_COUNT) {
                    call(case.resetInterval)
                }

                requests.awaitAll().count { it != null } shouldBe CONCURRENCY_COUNT
                delayedRequests.awaitAll().count { it == null } shouldBe CONCURRENCY_COUNT
            }
        }
    }

    private fun TestScope.initClient(useTestCoroutineScheduler: Boolean = true) {
        val mockEngine = MockEngine.create {
            if (useTestCoroutineScheduler) {
                dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            }
            requestHandlers.add { _ ->
                mockResponse()
            }
        }
        client = HttpClient(mockEngine) {
            install(CircuitBreaking) {
                global {
                    failureThreshold = Global.failureThreshold
                    halfOpenFailureThreshold = Global.halfOpenFailureThreshold
                    resetInterval = Global.resetInterval
                    failureTrigger = {
                        status.value >= 400
                    }
                }
                register(TestCircuitBreakerName) {
                    failureThreshold = Specific.failureThreshold
                    halfOpenFailureThreshold = Specific.halfOpenFailureThreshold
                    resetInterval = Specific.resetInterval
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

    private fun givenOkResponse() {
        mockResponse = {
            respond("Ok-ish", customOk)
        }
    }

    private val customOk = HttpStatusCode(300, "It's ok!")

    private suspend fun getWithCircuitBreaker(name: CircuitBreakerName) =
        client.requestWithCircuitBreaker(name = name) {}.status
}
