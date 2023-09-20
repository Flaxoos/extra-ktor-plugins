package io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets

import io.ktor.server.application.ApplicationCall
import kotlinx.datetime.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class TimeWindow(private val maxCallWeight: Double, internal val timeWindow: Duration) {
    private val timeWindowMs = timeWindow.inWholeMilliseconds
    private var timestamps = provideQueue<Pair<Long, Double>>()
    private var currentTotalWeight = 0.0

    suspend fun canAccept(call: ApplicationCall, bucketCapacityUnit: BucketCapacityUnit): BucketResponse {
        val now = now()
        val nowMs = now.toEpochMilliseconds()

        // Remove timestamps that are older than the time window
        val cutoff = nowMs - timeWindowMs
        val index = timestamps.binarySearch { if (it.first < cutoff) -1 else if (it.first > cutoff) 1 else 0 }
        val insertionPoint = if (index < 0) -index - 1 else index

        if (insertionPoint > 0) {
            val removed = timestamps.subList(0, insertionPoint)
            currentTotalWeight -= removed.sumOf { it.second }
            removed.clear()
        }

        val callSize = bucketCapacityUnit.callSize(call)

        if ((currentTotalWeight + callSize) <= maxCallWeight) {
            timestamps.add(nowMs to callSize)
            currentTotalWeight += callSize
            return BucketResponse.NotLimited
        }

        return BucketResponse.LimitedBy.TimeWindow(
            resetIn = (timeWindowMs - (nowMs - timestamps.first().first)).milliseconds,
            exceededBy = callSize,
            window = timeWindow,
            maxCallWeight = maxCallWeight
        )
    }
}


