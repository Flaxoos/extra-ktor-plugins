package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Token Bucket, allows varying rate of delivery,
 * Better for handling bursts, but can be exploited by malicious clients to consume all the capacity at once
 * The configured [Bucket.rate] will be triggered once per said
 * duration, allowing for bursts to consume the entire added capacity in a short time
 */
class TokenBucket(
    override val coroutineScope: CoroutineScope,
    override val log: KLogger = logger,
    /**
     * At what rate should token be added to the bucket
     */
    override val rate: Duration,
    override val capacity: Int,
    /**
     * The token to be added at every refill, can be [CallVolumeUnit.Calls] with optional call weight factor,
     * or [CallVolumeUnit.Bytes], specifying how many bytes to add at every refill
     */
    override val callVolumeUnit: CallVolumeUnit = CallVolumeUnit.Calls(),
) : Bucket() {
    private val capacityByTokenSize = capacity * callVolumeUnit.size.toDouble()
    private var currentVolume = capacityByTokenSize.also { log.debug { "Bucket initialized with volume: $it" } }
    private val currentVolumeLock = Mutex()

    init {
        volumeChangeJob.start()
    }

    override suspend fun changeVolume() {
        currentVolumeLock.withLock {
            val tokensToAdd = minOf(capacityByTokenSize - currentVolume, callVolumeUnit.size())
            if (tokensToAdd > 0) {
                currentVolume += tokensToAdd
                log.trace { "Bucket periodically refilled, added $tokensToAdd. current volume: $currentVolume" }
            }
        }
    }

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        val callSize = callVolumeUnit.callSize(call)
        return currentVolumeLock.withLock {
            if (currentVolume - callSize >= 0.0) {
                log.debug { "Bucket passing call and consumed $callSize: volume change: $currentVolume -> ${currentVolume - callSize}" }
                currentVolume -= callSize
                currentVolume
            } else
                null
        }?.let {
            RateLimiterResponse.NotLimited(this)
        } ?: RateLimiterResponse.LimitedBy(
            this@TokenBucket,
            resetIn = rate,
            exceededBy = callSize,
            message = "Insufficient tokens to accept call. tokens: $currentVolume, " +
                    "measured in ${callVolumeUnit::class.simpleName?.lowercase()} of size ${callVolumeUnit.size}. " +
                    "call size: $callSize"
        )
    }
}

