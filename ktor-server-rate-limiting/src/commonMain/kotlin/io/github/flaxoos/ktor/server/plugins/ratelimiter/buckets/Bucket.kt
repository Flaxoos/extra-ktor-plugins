package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketCapacityUnit.Bytes
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketCapacityUnit.Calls
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketType.Leaky
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketType.Token
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KtorSimpleLogger(Bucket::class.simpleName!!)

class Bucket(
    /**
     * The type of the bucket
     */
    val type: BucketType,

    /**
     * Desired token change over time
     */
    val volumeChangeRate: Pair<Duration, Double>,

    /**
     * The maximum volume of the bucket, measured in bytes if the token type is [BucketCapacityUnit.Bytes] or call weight if
     * the token type is [BucketCapacityUnit.Calls]
     */
    internal val capacity: Double,

    /**
     * The bucket token type
     */
    private val capacityUnit: BucketCapacityUnit,

    /**
     * An optional constraint on the number of requests per time window
     */
    val timeWindow: TimeWindow? = null,

    /**
     * Volume update coroutine scope
     */
    val volumeUpdateScope: CoroutineScope,

    ) {
    val volumeUpdateJob = volumeUpdateScope.launch {
        while (isActive) {
            delay(volumeChangeRate.first / type.volumeUpdatesPerChangeDuration)
            updateVolumeForPeriod(volumeChangeRate.second / type.volumeUpdatesPerChangeDuration)
        }
    }

    init {
        val maxDuration = Int.MAX_VALUE.milliseconds
        require(volumeChangeRate.first < maxDuration) {
            "volumeChangeRate must be less than Int.MAX_VALUE.milliseconds"
        }
        timeWindow?.let {
            require(it.timeWindow < maxDuration) {
                "timeWindow must be less than Int.MAX_VALUE.milliseconds"
            }
        }

    }

    private var currentVolume = when (type) {
        is Token -> capacity
        is Leaky -> 0
    }.toDouble().also {
        logger.info("$type bucket initialized with volume: $it")
    }

    private var lastCheck = now().toEpochMilliseconds()

    suspend fun handleCall(call: ApplicationCall): BucketResponse =
        timeWindow?.canAccept(call, capacityUnit)?.let { if (it is BucketResponse.LimitedBy) return@let it else null }
            ?: run {
                canHandleCall(call)
            }

    private fun updateVolumeForPeriod(volumeChange: Double) {
        val timePassedMs = now().toEpochMilliseconds() - lastCheck
        lastCheck = now().toEpochMilliseconds()
        val currentVolumeBeforeUpdate = 0 + currentVolume
        logger.info("timePassedMs: $timePassedMs, volumeChangeRate: $volumeChangeRate, change: $volumeChange")
        val weightedVolumeChange = (volumeChange *
                when (capacityUnit) {
                    is Calls -> 1
                    is Bytes -> capacityUnit.size
                }).coerceIn(0.0, capacity)
        logger.info("weightedVolumeChange: $weightedVolumeChange")
        currentVolume =
            when (type) {
                is Token -> currentVolume + weightedVolumeChange
                is Leaky -> currentVolume - weightedVolumeChange
            }.also {
                logger.info("$type bucket periodically updated volume from $currentVolumeBeforeUpdate to $currentVolume")
            }

    }

    private suspend fun canHandleCall(call: ApplicationCall): BucketResponse {
        val callSize = capacityUnit.callSize(call)
        return if (when (type) {
                is Token -> currentVolume > 0
                is Leaky -> currentVolume + callSize <= capacity
            }
        ) {
            val volumeChange = when (type) {
                is Token -> callSize * -1
                is Leaky -> callSize * 1
            }
            logger.info("$type bucket passing call and consumed $callSize: volume change: $currentVolume -> ${currentVolume + volumeChange}")
            currentVolume += volumeChange
            BucketResponse.NotLimited
        } else {
            BucketResponse.LimitedBy.Bucket(
                resetIn = volumeChangeRate.first,
                exceededBy = callSize,
                capacityLimit = capacity,
                bucketCapacityUnit = capacityUnit
            )
        }
    }
}

sealed class BucketType {
    abstract val volumeUpdatesPerChangeDuration: Int
    override fun toString(): String {
        return "${this::class.simpleName!!}${
            when (this) {
                is Leaky -> " ($volumeUpdatesPerChangeDuration)"
                is Token -> ""
            }
        }"
    }

    /**
     * Leaky Bucket, allows for constant rate of delivery,
     * Fair distribution between clients, but not ideal for handling bursts
     * The configured [Bucket.volumeChangeRate] will be triggered [volumeUpdatesPerChangeDuration] times per said
     * duration, dividing the added volume between the updates, and given a [volumeUpdatesPerChangeDuration] greater than 1, preventing request bursts,
     * leading to a constant rate of requests
     *
     * @param volumeUpdatesPerChangeDuration the number of times the [Bucket.volumeChangeRate] will be triggered
     * (with the amount divided by this figure). must be > 1 (if equal to 1, just use [Token])
     */
    data class Leaky(override val volumeUpdatesPerChangeDuration: Int) : BucketType()

    /**
     * Token Bucket, allows varying rate of delivery,
     * Better for handling bursts, but can be exploited by malicious clients to consume all the capacity at once
     * The configured [Bucket.volumeChangeRate] will be triggered once per said
     * duration, allowing for bursts to consume the entire added capacity in a short time
     */
    data object Token : BucketType() {
        override val volumeUpdatesPerChangeDuration = 1
    }
}

sealed class BucketCapacityUnit {
    data class Calls(val callWeighting: ApplicationCall.() -> Double = { 1.0 }) : BucketCapacityUnit()

    data class Bytes(val size: Int) : BucketCapacityUnit()

    @Suppress("LeakingThis")
    val measures = when (this) {
        is Calls -> "calls"
        is Bytes -> "bytes"
    }

    suspend fun callSize(
        call: ApplicationCall
    ) = when (this) {
        is Calls -> this.callWeighting(call)
        is Bytes -> call.receive(ByteArray::class).size.toDouble()
    }
}


sealed interface BucketResponse {

    data object NotLimited : BucketResponse

    sealed interface LimitedBy : BucketResponse {
        val message: String
        val exceededBy: Double
        val resetIn: Duration

        data class TimeWindow(
            override val resetIn: Duration,
            override val exceededBy: Double,
            val window: Duration,
            val maxCallWeight: Double
        ) : LimitedBy {
            override val message: String =
                "$maxCallWeight calls were already made during $window"
        }

        data class Bucket(
            override val resetIn: Duration,
            override val exceededBy: Double,
            val capacityLimit: Double,
            val bucketCapacityUnit: BucketCapacityUnit,
        ) : LimitedBy {
            override val message: String =
                "capacity of $capacityLimit ${bucketCapacityUnit.measures} exceeded by $exceededBy"
        }
    }
}