@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)

package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import io.github.flaxoos.ktor.server.plugins.ratelimiter.CallVolumeUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiter
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiterResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

const val capacity = 5
const val rateSeconds = 1
const val bytesToAdd = 10
const val callSize = 10
const val callWeight = 1.0
const val leakGraceMs = 1
private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalTime::class)
class RateLimiterTest : FunSpec() {
    private val callVolumeUnit = mockk<CallVolumeUnit.Calls> {
        every { size } returns 1
        every { size() } returns 1.0
    }
    private val bytesVolumeUnit = CallVolumeUnit.Bytes(bytesToAdd)

    private fun mockCall(sizeAndWeight: Pair<Int, Double>, index: Int): ApplicationCall = mockk(relaxed = true) {
        coEvery { receive<Any>() } returns sizeAndWeight.first
        coEvery { this@mockk.toString() } returns "Test call: $index"
        coEvery { attributes } returns Attributes()
        every { request } returns mockk(relaxed = true) {
            every { pipeline } returns mockk {
                coEvery { execute(any(), any()) } returns ByteArray(sizeAndWeight.first)
            }
        }
        coEvery { callVolumeUnit.callSize(this@mockk) } returns sizeAndWeight.second
    }

    private val callIndexAttributeKey = AttributeKey<Int>("callIndex")

    init {
        coroutineTestScope = true
        invocationTimeout = 30.seconds.inWholeMilliseconds
        coroutineDebugProbes = false

        context("Rate Limiter Provider tests") {
            withData(
                nameFn = { it.simpleName!! },
                listOf(
                    TokenBucket::class,
                    LeakyBucket::class,
                    SlidingWindow::class
                )
            ) { type ->
                withData(
                    nameFn = {
                        "Measure by ${it::class.simpleName!!} should deny calls when ${
                        when (type) {
                            TokenBucket::class -> "empty"
                            LeakyBucket::class -> "full"
                            SlidingWindow::class -> "full"
                            else -> error("Unknown provider type: $type")
                        }
                        }"
                    },
                    listOf(callVolumeUnit, bytesVolumeUnit)
                ) { unit ->
                    val provider = spyk(provider(type, unit))
                    if (coroutineTestScope == true) testCoroutineScheduler.runCurrent()

                    // calls before limit should pass (token bucket / sliding window last call is double sized), next should fail
                    val invocations = capacity.plus(
                        when (provider) {
                            is TokenBucket, is SlidingWindow -> 0
                            is LeakyBucket -> 1
                            else -> error("Unknown provider type: $type")
                        }
                    )

                    makeCalls(
                        rateLimiter = provider,
                        invocations = invocations,
                        differentSizeAndWeightOn = mapOf(invocations - 1 to (callSize * 2 to callWeight * 2))
                    ) { callIndex, theProvider ->
                        if (callIndex < invocations) {
                            shouldNotBeLimited()
                        } else {
                            shouldBeLimitedBy(theProvider)
                        }
                    }

                    if (provider is TokenBucket) {
                        // accumulate tokens
                        logger.info { "Accumulating $capacity tokens" }
                        testCoroutineScheduler.advanceTimeBy(rateSeconds.seconds * capacity)
                    } else if (provider is SlidingWindow) {
                        // let window slide
                        logger.info { "Waiting for window to slide" }
                        testCoroutineScheduler.advanceTimeBy(rateSeconds.seconds)
                    }

                    // fire burst
                    logger.info { "Firing burst of $capacity calls" }
                    val beforeBurst = testCoroutineScheduler.timeSource.markNow()
                    makeCalls(
                        rateLimiter = provider,
                        invocations = capacity,
                        burst = true
                    )
                    beforeBurst.elapsedNow().apply {
                        when (provider) {
                            is LeakyBucket -> shouldBe((rateSeconds.seconds * capacity) + leakGraceMs.milliseconds)
                            is TokenBucket -> shouldBeLessThan(rateSeconds.seconds)
                            is SlidingWindow -> shouldBeLessThan(rateSeconds.seconds)
                        }
                    }

                    makeCalls(
                        rateLimiter = provider,
                        invocations = 1,
                        burst = true
                    ) { _, theProvider ->
                        when (provider) {
                            is LeakyBucket -> shouldNotBeLimited()
                            // tokens should be exhausted
                            is TokenBucket -> shouldBeLimitedBy(theProvider)
                            is SlidingWindow -> shouldBeLimitedBy(theProvider)
                        }
                    }
                }
            }
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun TestScope.makeCalls(
        rateLimiter: RateLimiter,
        invocations: Int,
        defaultSizeAndWeight: Pair<Int, Double> = callSize to callWeight,
        differentSizeAndWeightOn: Map<Int, Pair<Int, Double>> = emptyMap(),
        burst: Boolean = false,
        checkForCallIndex: RateLimiterResponse.(Int, RateLimiter) -> Unit = { _, _ -> this.shouldNotBeLimited() }
    ) {
        val results = (1..invocations).map { callIndex ->
            val sizeAndWeight = differentSizeAndWeightOn[callIndex] ?: defaultSizeAndWeight
            val call = mockCall(sizeAndWeight, callIndex)
            call.attributes.put(callIndexAttributeKey, callIndex)
            async {
                val response =
                    runCatching {
                        if (rateLimiter is LeakyBucket) {
                            // make sure all calls are added to bucket before leaks start
                            coEvery { rateLimiter.tryIncreaseVolume(any(), any(), any(), any()) } coAnswers {
                                val response = this.callOriginal()
                                delay(leakGraceMs.milliseconds)
                                response
                            }
                        }
                        rateLimiter.tryAccept(call)
                    }.getOrElse {
                        fail("Call index $callIndex: ${it.message}", it)
                    }
                return@async {
                    withClue("Call index $callIndex, response: $response") {
                        response.checkForCallIndex(callIndex, rateLimiter)
                    }
                }
            }
        }

        results.map {
            launch {
                if (!burst) delay(rateSeconds.seconds)
                it.await().invoke()
            }
        }.joinAll()
    }

    private fun RateLimiterResponse.shouldBeLimitedBy(provider: RateLimiter) {
        shouldBeTypeOf<RateLimiterResponse.LimitedBy>()
        this.rateLimiter shouldBe provider
    }

    private fun RateLimiterResponse.shouldNotBeLimited() {
        shouldBeTypeOf<RateLimiterResponse.NotLimited>()
    }

    private fun TestScope.provider(
        type: KClass<out RateLimiter>,
        unit: CallVolumeUnit
    ): RateLimiter {
        return when (type) {
            TokenBucket::class -> {
                TokenBucket(
                    rate = rateSeconds.seconds,
                    capacity = capacity,
                    callVolumeUnit = unit,
                    clock = { testCoroutineScheduler.currentTime }
                )
            }

            LeakyBucket::class -> {
                LeakyBucket(
                    rate = rateSeconds.seconds,
                    capacity = capacity,
                    clock = { testCoroutineScheduler.currentTime }
                )
            }

            SlidingWindow::class -> {
                SlidingWindow(
                    rate = rateSeconds.seconds,
                    capacity = capacity,
                    callVolumeUnit = unit,
                    clock = { testCoroutineScheduler.currentTime }
                )
            }

            else -> error("Unknown provider type: $type")
        }
    }
}
