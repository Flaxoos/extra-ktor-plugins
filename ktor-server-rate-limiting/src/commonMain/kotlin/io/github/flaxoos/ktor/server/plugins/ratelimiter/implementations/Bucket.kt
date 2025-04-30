package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiter
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.server.application.ApplicationCall
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds

sealed class Bucket(
    initialVolume: Int,
    final override val clock: () -> Long,
    final override val capacity: Int,
) : RateLimiter() {
    internal abstract val log: KLogger

    internal var currentVolume =
        initialVolume
            .toDouble()
            .also {
                require(0 <= it && it <= capacity) {
                    "Volume $initialVolume must be between 0 and $capacity"
                }
            }
        private set

    private val currentVolumeMutex = Mutex()

    @Suppress("ktlint:standard:backing-property-naming")
    private val _lastUpdateTime = atomic(clock())

    /**
     * Reduces the volume by the given amount if it won't lead to a negative volume
     *
     * @param shouldUpdateTime whether to update the last update time
     * @param by the amount to reduce the volume by, must be positive
     */
    internal suspend fun tryReduceVolume(
        call: ApplicationCall,
        by: Double,
        shouldUpdateTime: Boolean = false,
        consideringLastUpdateTime: suspend Double.(Long) -> Double = { this },
    ): Double? {
        by.checkNotNegative()
        return reduceVolume(call, by, shouldUpdateTime, consideringLastUpdateTime)
    }

    /**
     * Reduces the volume by the given amount, coercing to at least 0.0
     *
     * @param shouldUpdateTime whether to update the last update time
     * @param by the amount to reduce the volume by, must be positive
     */
    internal suspend fun reduceVolume(
        call: ApplicationCall,
        by: Double,
        shouldUpdateTime: Boolean = false,
        consideringLastUpdateTime: suspend Double.(Long) -> Double = { this },
    ): Double? {
        by.checkNotNegative()
        return tryUpdateVolume(call, shouldUpdateTime) { volume, timeSinceLastUpdate ->
            volume - by.consideringLastUpdateTime(timeSinceLastUpdate).coerceAtLeast(0.0)
        }
    }

    /**
     * Increases the volume by the given amount if it won't lead to a volume exceeding the capacity
     *
     * @param shouldUpdateTime whether to update the last update time
     * @param by the amount to increase the volume by, must be positive
     */
    internal suspend fun tryIncreaseVolume(
        call: ApplicationCall,
        by: Double,
        shouldUpdateTime: Boolean = false,
        consideringLastUpdateTime: suspend Double.(Long) -> Double = { this },
    ): Double? {
        by.checkNotNegative()
        return increaseVolume(call, by, shouldUpdateTime, consideringLastUpdateTime)
    }

    /**
     * Increases the volume by the given amount, coercing to at most the capacity
     *
     * @param shouldUpdateTime whether to update the last update time
     * @param by the amount to increase the volume by, must be positive
     */
    internal suspend fun increaseVolume(
        call: ApplicationCall,
        by: Double,
        shouldUpdateTime: Boolean = false,
        consideringLastUpdateTime: suspend Double.(Long) -> Double = { this },
    ): Double? {
        by.checkNotNegative()
        return tryUpdateVolume(call, shouldUpdateTime) { volume, timeSinceLastUpdate ->
            volume + (consideringLastUpdateTime(by, timeSinceLastUpdate)).coerceAtMost(capacity.toDouble())
        }
    }

    /**
     * Tries to update the volume if the updated volume is within the capacity of the bucket
     *
     * @param update the function to update the volume, with the current volume and the time since the last update as
     * the arguments
     * @return the updated volume if it is within the capacity of the bucket,
     * or null if the updated volume is not within the capacity
     */
    private suspend fun tryUpdateVolume(
        call: ApplicationCall,
        shouldUpdateTime: Boolean = false,
        update: suspend (Double, Long) -> Double,
    ): Double? =
        currentVolumeMutex.withLock {
            val now = clock()
            val lastUpdateTime = _lastUpdateTime.value
            val timeSinceLastUpdate = now - lastUpdateTime
            log.trace {
                "${call.id()}: Maybe updating volume at now: ${fromEpochMilliseconds(now)}, " +
                    "lastUpdateTime: ${fromEpochMilliseconds(lastUpdateTime)}, " +
                    "diff: $timeSinceLastUpdate"
            }

            val updatedVolume = update(currentVolume, timeSinceLastUpdate)
            val diff = updatedVolume - currentVolume

            if (
                0 <= updatedVolume &&
                updatedVolume <= capacity * callVolumeUnit.size
            ) {
                log.trace { "${call.id()}: Updating volume from $currentVolume to $updatedVolume" }
                currentVolume = updatedVolume
                if (shouldUpdateTime) {
                    _lastUpdateTime.update { now }
                }
                currentVolume
            } else {
                log.trace {
                    "${call.id()}: Volume not updated due to exceeding volume: currentVolume: $currentVolume, updatedVolume: $updatedVolume, diff: $diff, max: ${capacity * callVolumeUnit.size}"
                }
                null
            }
        }

    private fun Double.checkNotNegative(): Double {
        check(this >= 0) { "Volume change figure must not be negative. was: $this" }
        return this
    }
}
