@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)

package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.datatest.withData
import io.kotest.engine.test.logging.LogEntry
import io.kotest.engine.test.logging.LogExtension
import io.kotest.engine.test.logging.info
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.server.application.ApplicationCall
import io.ktor.util.logging.KtorSimpleLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val bucketCapacity = 10
const val leakRate = 10
const val volumeChangeRateSeconds = 1
const val bytesToAdd = 10
const val volumeChange = 1.0
const val callASizeInBytes = 10
const val callBSizeInBytes = 20
const val callAWeight = 1.0
const val callBWeight = 2.0
const val accumulate = 2

fun BucketCapacityUnit.weightFactor() = when (this) {
    is BucketCapacityUnit.Calls -> callAWeight
    is BucketCapacityUnit.Bytes -> callASizeInBytes.toDouble()
}

val logger = KtorSimpleLogger("BucketTest")

@OptIn(ExperimentalKotest::class, ExperimentalStdlibApi::class)
class BucketTest : FunSpec() {

    init {
        coroutineTestScope = true
        coroutineDebugProbes = true
        val callA: ApplicationCall = mockk()
        val callB: ApplicationCall = mockk()
        val bucketCallUnit = mockk<BucketCapacityUnit.Calls> {
            coEvery { callSize(callA) } returns callAWeight
            coEvery { callSize(callB) } returns callBWeight
            every { measures } returns "calls"
        }
        val bucketBytesUnit = mockk<BucketCapacityUnit.Bytes> {
            coEvery { size } returns bytesToAdd
            coEvery { callSize(callA) } returns callASizeInBytes.toDouble()
            coEvery { callSize(callB) } returns callBSizeInBytes.toDouble()
            every { measures } returns "bytes"
        }
        val timeWindow = volumeChangeRateSeconds.seconds
        val capacityRestrictiveWindow = { factor: Double -> TimeWindow(bucketCapacity * factor, timeWindow) }
        val permissiveWindow = { factor: Double -> TimeWindow(bucketCapacity * 1.1 * factor, timeWindow) }




        context("Bucket tests") {
            withData(
                nameFn = { "${it::class.simpleName!!} bucket" },
                listOf(BucketType.Token, BucketType.Leaky(leakRate))
            ) { bucketType ->
                withData(
                    nameFn = { "Measure by ${it::class.simpleName!!}" },
                    listOf(bucketCallUnit, bucketBytesUnit)
                ) { unit ->
                    withData(
                        nameFn = {
                            "With ${
                                when (it) {
                                    capacityRestrictiveWindow -> "Restrictive window"
                                    permissiveWindow -> "Permissive window"
                                    else -> "No window"
                                }
                            } should deny calls when ${
                                when (bucketType) {
                                    is BucketType.Token -> "empty"
                                    is BucketType.Leaky -> "full"
                                }
                            }"
                        },
                        listOf<((Double) -> TimeWindow)?>(
                            null, permissiveWindow, capacityRestrictiveWindow
                        )
                    ) { window ->
                        withData(nameFn = { "Burst: $it" }, first = true, second = false) { burst ->
                            val bucket = createBucket(bucketType, unit, window)

                            val invocations = (0..bucketCapacity - callBWeight.toInt())
                            invocations.forEach { callIndex ->
                                val response =
                                    runCatching { bucket.handleCall(if (callIndex != invocations.last) callA else callB) }.getOrElse {
                                        throw Exception("Call index $callIndex: ${it.message}", it)
                                    }
                                withClue("Call index $callIndex, response: $response") {
                                    response.shouldNotBeLimited()
                                }
                            }

                            val response = bucket.handleCall(callA)
                            if (window != capacityRestrictiveWindow)
                                response.shouldBeLimitedByBucket()
                            else
                                response.shouldBeLimitedByWindow()

                            // accumulate tokens
                            info { "Accumulating $accumulate tokens" }
                            delay(volumeChangeRateSeconds.seconds * accumulate)

                            // Grace period for refill timing inaccuracies
                            if (bucketType == BucketType.Token) delay(100.milliseconds)

                            if (burst) {
                                // fire burst
                                info { "Firing burst of $accumulate calls" }
                                repeat(accumulate) {
                                    with(bucket.handleCall(callA)) {
                                        when {
                                            bucket.timeWindow != null -> shouldBeLimitedByWindow()
                                            it == 0 -> shouldNotBeLimited() //First should pass in both cases
                                            bucketType is BucketType.Token -> shouldNotBeLimited()
                                            bucketType is BucketType.Leaky -> shouldBeLimitedByBucket()
                                        }
                                    }
                                }
                                if (bucket.timeWindow != null) {
                                    response.shouldBeLimitedByWindow()
                                } else {
                                    bucket.handleCall(callA).shouldBeLimitedByBucket()
                                }
                            }

                            runCatching { bucket.volumeUpdateJob.cancel() }
                        }
                    }
                }
            }
        }
    }

    private fun BucketResponse.shouldBeLimitedByBucket() {
        shouldBeTypeOf<BucketResponse.LimitedBy.Bucket>()
    }

    private fun BucketResponse.shouldBeLimitedByWindow() {
        shouldBeTypeOf<BucketResponse.LimitedBy.TimeWindow>()
    }

    private fun BucketResponse.shouldNotBeLimited() {
        shouldBeTypeOf<BucketResponse.NotLimited>()
    }

    private fun ContainerScope.createBucket(
        type: BucketType,
        unit: BucketCapacityUnit,
        window: ((Double) -> TimeWindow)?
    ) = Bucket(
        type = type,
        volumeChangeRate = volumeChangeRateSeconds.seconds to volumeChange,
        capacity = bucketCapacity * unit.weightFactor(),
        capacityUnit = unit,
        timeWindow = window?.let { it(unit.weightFactor()) },
        volumeUpdateScope = this + StandardTestDispatcher(testCoroutineScheduler),
    )
}

object KotestLogConfig : AbstractProjectConfig() {
    override val logLevel = LogLevel.Info
    override fun extensions() = listOf(Log)
}

@OptIn(ExperimentalKotest::class)
object Log : LogExtension {
    override suspend fun handleLogs(testCase: TestCase, logs: List<LogEntry>) {
        logs.forEach { logger.info(it.message.toString()) }
    }
}

suspend fun TestScope.delayAndAdvance(duration: Duration) =
    delay(duration).also { testCoroutineScheduler.advanceUntilIdle() }