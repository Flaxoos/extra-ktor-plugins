package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}


data class SlidingWindow(
    override val rate: Duration,
    override val capacity: Int,
    override val callVolumeUnit: CallVolumeUnit,
    private val clock: () -> Long = { now().toEpochMilliseconds() }
) : RateLimitProvider {
    private val timeWindowMs = rate.inWholeMilliseconds
    private var timestamps = ConcurrentFixedSizeWeightedQueue<Long>(capacity * callVolumeUnit.size)

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        val now = clock()
        logger.debug { "Call attempted at time: ${fromEpochMilliseconds(now)}, timestamps: $timestamps" }

        updateWeight(now)

        val callSize = callVolumeUnit.callSize(call)
        if (timestamps.tryAdd(now, callSize)) return RateLimiterResponse.NotLimited(this).also {
            logger.debug { "Passing call with size: $callSize" }
        }

        val nextTimestampToBeTrimmed =
            (timestamps.peekNext()?.first ?: error("Queue is empty but call was rejected"))
        val resetIn = (timeWindowMs - (now - nextTimestampToBeTrimmed)).milliseconds
        logger.debug { "Call rejected. reset in: $resetIn" }
        return RateLimiterResponse.LimitedBy(
            this,
            resetIn = resetIn,
            exceededBy = callSize,
            message = "$capacity calls were already made during $rate",
        )
    }

    override fun stop() {}

    private fun updateWeight(nowMs: Long) {
        val cutoff = nowMs - timeWindowMs
        logger.debug { "Trimming timestamps before: ${fromEpochMilliseconds(cutoff)}" }
        timestamps.trimWhere { timestamp ->
            if (timestamp < cutoff) -1 else if (timestamp > cutoff) 1 else 0
        }
    }
}


internal expect fun <T> provideListForQueue(): MutableList<T>

/**
 * A fixed size queue of weighted entries with safe access
 */
class ConcurrentFixedSizeWeightedQueue<T>(
    /**
     * the maximum weight, must be greater than 0
     */
    private val maxWeight: Int
) {
    private val list = provideListForQueue<Pair<T, Double>>()
    private val size: Int
        get() = list.size
    private var weight = 0.0
    private val lock = reentrantLock()
    internal fun asFlow() = list.asFlow().map { it.first }

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
        } else false
    }

    fun removeIfNext(t: T): Boolean =
        lock.withLock {
            if (list.firstOrNull() == t) {
                list.removeFirst()
                true
            } else false
        }

    fun removeNext(): T? =
        lock.withLock {
            val next = list.firstOrNull()?.first
            list.removeFirst()
            next
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
        } else null
    }
}