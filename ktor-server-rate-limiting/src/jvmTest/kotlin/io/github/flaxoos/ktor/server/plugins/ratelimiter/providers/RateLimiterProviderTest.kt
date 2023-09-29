@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)

package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

const val capacity = 10
const val rateSeconds = 1
const val bytesToAdd = 10
const val callSize = 10
const val callWeight = 1.0

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalTime::class)
class RateLimitProviderTest : FunSpec() {
    private val callVolumeUnit = mockk<CallVolumeUnit.Calls> {
        every { size } returns 1
        every { size() } returns 1.0
    }
    private val bytesVolumeUnit = CallVolumeUnit.Bytes(bytesToAdd)

    private fun mockCall(sizeAndWeight: Pair<Int, Double>): ApplicationCall = mockk(relaxed = true) {
        coEvery { receive<Any>() } returns sizeAndWeight.first
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
        timeout = 30.seconds.inWholeMilliseconds
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
                    val provider = provider(type, unit)
                    testCoroutineScheduler.advanceTimeBy(rateSeconds.seconds)
                    if (coroutineTestScope == true) testCoroutineScheduler.runCurrent()

                    // calls before limit should pass (token bucket / sliding window last call is double sized), next should fail
                    val invocations = capacity.plus(
                        when (provider) {
                            is TokenBucket, is SlidingWindow -> 0
                            is LeakyBucket -> 1
                        }
                    )

                    makeCalls(
                        rateLimitProvider = provider,
                        invocations = invocations,
                        differentSizeAndWeightOn = mapOf(invocations - 1 to (callSize * 2 to callWeight * 2)),
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
                        rateLimitProvider = provider,
                        invocations = capacity,
                        burst = true
                    )
                    beforeBurst.elapsedNow().apply {
                        when (provider) {
                            is LeakyBucket -> shouldBe(rateSeconds.seconds * capacity)
                            is TokenBucket -> shouldBeLessThan(rateSeconds.seconds)
                            is SlidingWindow -> shouldBeLessThan(rateSeconds.seconds)
                        }
                    }

                    makeCalls(
                        rateLimitProvider = provider,
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
                    if (provider is Bucket) provider.stop()
                }
            }
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun TestScope.makeCalls(
        rateLimitProvider: RateLimitProvider,
        invocations: Int,
        defaultSizeAndWeight: Pair<Int, Double> = callSize to callWeight,
        differentSizeAndWeightOn: Map<Int, Pair<Int, Double>> = emptyMap(),
        burst: Boolean = false,
        checkForCallIndex: RateLimiterResponse.(Int, RateLimitProvider) -> Unit = { _, _ -> this.shouldNotBeLimited() }
    ) {
        val results = (1..invocations).map { callIndex ->
            val sizeAndWeight = differentSizeAndWeightOn[callIndex] ?: defaultSizeAndWeight
            val call = mockCall(sizeAndWeight)
            call.attributes.put(callIndexAttributeKey, callIndex)
            async {
                val response =
                    runCatching {
                        logger.info { "Trying call: $callIndex" }
                        rateLimitProvider.tryAccept(call)
                    }.getOrElse {
                        if (rateLimitProvider is Bucket) rateLimitProvider.stop()
                        fail("Call index $callIndex: ${it.message}", it)
                    }
                return@async {
                    withClue("Call index $callIndex, response: $response") {
                        response.checkForCallIndex(callIndex, rateLimitProvider)
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


    private fun RateLimiterResponse.shouldBeLimitedBy(provider: RateLimitProvider) {
        shouldBeTypeOf<RateLimiterResponse.LimitedBy>()
        this.provider shouldBe provider
    }

    private fun RateLimiterResponse.shouldNotBeLimited() {
        shouldBeTypeOf<RateLimiterResponse.NotLimited>()

    }

    private fun TestScope.provider(
        type: KClass<out RateLimitProvider>,
        unit: CallVolumeUnit
    ): RateLimitProvider {
        return when (type) {
            TokenBucket::class -> {
                TokenBucket(
                    coroutineScope = this,
                    rate = rateSeconds.seconds,
                    capacity = capacity,
                    callVolumeUnit = unit
                )
            }

            LeakyBucket::class -> {
                LeakyBucket(
                    coroutineScope = this,
                    rate = rateSeconds.seconds,
                    capacity = capacity,
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
