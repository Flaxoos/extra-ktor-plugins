package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import io.github.flaxoos.ktor.server.plugins.ratelimiter.CallVolumeUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiterResponse
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiterResponse.LimitedBy
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Leaky Bucket, allows for constant rate of delivery,
 * Fair distribution between clients, but not ideal for handling bursts
 *
 * The configured [rate] will be the rate in which requests will leak from the bucket, when not empty.
 * This means the call will be suspended until it leaks out of the bucket, at which point the [tryAccept] function would
 * return. No timeout is set, so the call can be suspended indefinitely in theory, and it is up to the server or client
 * to implement a timeout.
 *
 * CallVolumeUnit is always Calls with weighting of 1.0.
 */
class LeakyBucket(
    override val log: KLogger = logger,
    override val rate: Duration,
    /**
     * The maximum capacity, as measured in [CallVolumeUnit.Calls]
     */
    capacity: Int,
    /**
     * A time provider in milliseconds
     */
    clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : Bucket(0, clock, capacity) {
    private val leakHole = Mutex()
    private var lastLeak: Long = Instant.DISTANT_PAST.toEpochMilliseconds()
    override val callVolumeUnit: CallVolumeUnit = CallVolumeUnit.Calls { 1.0 }

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        log.debug { "${call.id()}: Trying to add call" }
        val callSize = callVolumeUnit.callSize(call)
        return tryIncreaseVolume(call, by = callSize)?.let { _ ->
            leak(call)
            RateLimiterResponse.NotLimited(
                this@LeakyBucket,
                remaining = null,
            )
        } ?: run {
            log.debug { "${call.id()}: Rejected due to bucket overflow" }
            LimitedBy(
                this,
                resetIn = rate,
                exceededBy = 1,
                message = "Bucket of size $capacity is full, call rejected",
            )
        }
    }

    private suspend fun leak(call: ApplicationCall) {
        leakHole.withLock {
            val now = clock()
            logger.debug { "${call.id()}: Leaking at: ${fromEpochMilliseconds(now)}" }
            val sinceLastLeak = (now - lastLeak)
            val wait = if (sinceLastLeak > rate.inWholeMilliseconds) 0 else rate.inWholeMilliseconds
            logger.debug { "${call.id()}: Waiting $wait before leaking" }
            delay(wait)
            logger.debug { "${call.id()}: Leaked at: ${fromEpochMilliseconds(lastLeak)}" }
            coroutineScope {
                launch {
                    delay(rate - wait.milliseconds)
                    lastLeak = now + rate.inWholeMilliseconds
                    reduceVolume(call, by = callVolumeUnit.callSize(call))
                        ?: error("Leak caused negative volume, this shouldn't happen")
                }
            }
        }
    }
}
