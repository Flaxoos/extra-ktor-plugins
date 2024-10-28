package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import io.github.flaxoos.ktor.server.plugins.ratelimiter.CallVolumeUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiterResponse
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Token Bucket, allows varying rate of delivery,
 * Better for handling bursts, but can be exploited by malicious clients to consume all the capacity at once.
 *
 * The configured [Bucket.rate] will be the rate at which tokens will be added to the bucket, until full.
 *
 * CallVolumeUnit can be [CallVolumeUnit.Calls] with optional call weight factor, or [CallVolumeUnit.Bytes],
 * specifying how many bytes to add at every refill
 */
class TokenBucket(
    override val log: KLogger = logger,
    /**
     * At what rate should token be added to the bucket
     */
    override val rate: Duration,

    /**
     * The token to be added at every refill, can be [CallVolumeUnit.Calls] with optional call weight factor,
     * or [CallVolumeUnit.Bytes], specifying how many bytes to add at every refill
     */
    override val callVolumeUnit: CallVolumeUnit = CallVolumeUnit.Calls(),

    /**
     * The maximum capacity, as measured in the specified [CallVolumeUnit]
     */
    capacity: Int,
    /**
     * A time provider in milliseconds
     */
    clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : Bucket(capacity * callVolumeUnit.size, clock, capacity * callVolumeUnit.size) {

    init {
        log.debug { "Initialized TokenBucket with volume: $currentVolume" }
    }

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        addTokensForDuration(call)

        val callSize = callVolumeUnit.callSize(call)
        log.debug { "${call.id()}: Trying to take tokens  $callSize tokens, current tokens: $currentVolume" }
        return tryReduceVolume(call, by = callSize)?.let {
            log.debug { "${call.id()}: accepted using $callSize tokens" }
            RateLimiterResponse.NotLimited(this)
        } ?: run {
            log.debug { "${call.id()}: rejected due to insufficient tokens" }
            RateLimiterResponse.LimitedBy(
                this@TokenBucket,
                resetIn = rate,
                exceededBy = callSize,
                message = "Insufficient tokens to accept call. tokens: $currentVolume, " +
                    "measured in ${callVolumeUnit::class.simpleName?.lowercase()} of size ${callVolumeUnit.size}. " +
                    "call size: $callSize",
            )
        }
    }

    private suspend fun addTokensForDuration(call: ApplicationCall) {
        logger.debug { "${call.id()}: Maybe adding tokens" }
        increaseVolume(call, shouldUpdateTime = true, by = callVolumeUnit.size.toDouble()) { timeSinceLastUpdate ->
            logger.debug { "${call.id()}: Checking if should add tokens: ${Instant.fromEpochMilliseconds(clock())}, time since last update: $timeSinceLastUpdate" }
            if (this > 0) {
                ((timeSinceLastUpdate / rate.inWholeMilliseconds) * this) * callVolumeUnit.size()
            } else {
                0.0
            }
        }?.let {
            logger.debug { "${call.id()}: Added $it tokens" }
        } ?: logger.debug { "${call.id()}: No tokens were added" }
    }
}
