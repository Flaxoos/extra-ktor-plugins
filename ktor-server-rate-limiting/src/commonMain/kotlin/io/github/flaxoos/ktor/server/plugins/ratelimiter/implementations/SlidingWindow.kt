package io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations

import io.github.flaxoos.common.queueList
import io.github.flaxoos.ktor.server.plugins.ratelimiter.CallVolumeUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiter
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiterResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Sliding window, allows a given weight of calls to be made over a given duration.
 *
 * The configured [rate] will be the time window over which the calls will be counted.
 * The call weight is defined by the [callVolumeUnit]
 */
data class SlidingWindow(
    override val rate: Duration,
    override val capacity: Int,
    override val callVolumeUnit: CallVolumeUnit,
    /**
     * A time provider
     */
    override val clock: () -> Long = { now().toEpochMilliseconds() }
) : RateLimiter() {
    private val timeWindowMs = rate.inWholeMilliseconds
    private var timestamps = ConcurrentFixedSizeWeightedQueue<Long>(capacity * callVolumeUnit.size)

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        val now = clock()
        logger.debug { "${call.id()}: Attempted at: $now, timestamps: $timestamps" }

        updateWeight(now)

        val callSize = callVolumeUnit.callSize(call)
        if (timestamps.tryAdd(now, callSize)) {
            return RateLimiterResponse.NotLimited(this).also {
                logger.debug { "${call.id()}: Passed with size: $callSize" }
            }
        }

        val nextTimestampToBeTrimmed =
            (timestamps.peekNext()?.first ?: error("Queue is empty but call was rejected"))
        val resetIn = (timeWindowMs - (now - nextTimestampToBeTrimmed)).milliseconds
        logger.debug { "${call.id()}: Rejected. reset in: $resetIn" }
        return RateLimiterResponse.LimitedBy(
            this,
            resetIn = resetIn,
            exceededBy = callSize,
            message = "$capacity calls were already made during $rate"
        )
    }

    private fun updateWeight(nowMs: Long) {
        val cutoff = nowMs - timeWindowMs
        logger.debug { "Trimming timestamps before: $cutoff" }
        timestamps.trimWhere { timestamp ->
            if (timestamp < cutoff) -1 else if (timestamp > cutoff) 1 else 0
        }
    }
}

/**
 * A fixed size queue of weighted entries with safe access
 */
class ConcurrentFixedSizeWeightedQueue<T>(
    /**
     * the maximum weight, must be greater than 0
     */
    private val maxWeight: Int
) {
    private val list = queueList<Pair<T, Double>>()
    private val size: Int
        get() = list.size
    private var weight = 0.0
    private val lock = reentrantLock()

    override fun toString(): String {
        return "ConcurrentFixedSizeWeightedQueue(size=$size, weight=$weight, maxWeight=$maxWeight)"
    }

    init {
        require(maxWeight > 0) {
            "maxWeight must be greater than 0"
        }
    }

    fun tryAdd(t: T, weight: Double = 1.0) = lock.withLock {
        if (this.weight + weight <= maxWeight) {
            this.weight += weight
            list.add(t to weight).also {
                logger.debug { "Added $t with weight $weight, current weight is now ${this.weight}" }
            }
        } else {
            false
        }
    }

    fun peekNext() = list.firstOrNull()

    fun trimWhere(comparison: (T) -> Int) = lock.withLock {
        subListWhere(comparison)?.apply {
            val toTrim = sumOf { it.second }
            logger.debug { "Trimmed ${this.size} items, weighing: $toTrim from queue with weight $weight" }
            weight -= toTrim
            clear()
        }
    }

    private fun subListWhere(comparison: (T) -> Int): MutableList<Pair<T, Double>>? {
        val index = lock.withLock {
            list.binarySearch {
                comparison(it.first)
            }
        }
        val insertionPoint = if (index < 0) -index - 1 else index
        return if (insertionPoint > 0) {
            list.subList(0, insertionPoint)
        } else {
            null
        }
    }
}
