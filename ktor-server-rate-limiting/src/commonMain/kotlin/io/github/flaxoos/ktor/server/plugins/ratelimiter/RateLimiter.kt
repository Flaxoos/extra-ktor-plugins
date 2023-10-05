package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.datetime.Clock.System.now
import kotlin.jvm.JvmInline
import kotlin.time.Duration

abstract class RateLimiter {

    /**
     * Desired change over time
     */
    abstract val rate: Duration

    /**
     * The maximum capacity, as measured in [CallVolumeUnit.Calls] or [CallVolumeUnit.Bytes]
     */
    abstract val capacity: Int

    /**
     * In what unit are calls measured
     */
    abstract val callVolumeUnit: CallVolumeUnit

    /**
     * A time provider in milliseconds
     */
    open val clock: () -> Long = { now().toEpochMilliseconds() }

    /**
     * Try to accept a call
     *
     * @param call the call to accept
     * @return response detailing if the call was accepted and details in the case of rejection
     */
    abstract suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse

    fun ApplicationCall.id(): String = this.request.headers[HttpHeaders.XRequestId] ?: this.toString()
}

sealed interface RateLimiterResponse {
    val rateLimiter: RateLimiter

    data class NotLimited(
        override val rateLimiter: RateLimiter,
        val remaining: Number? = null
    ) : RateLimiterResponse

    data class LimitedBy(
        override val rateLimiter: RateLimiter,
        val exceededBy: Number,
        val resetIn: Duration,
        val message: String
    ) : RateLimiterResponse
}

interface CallVolumeUnit {
    val name: String
    val size: Int

    /**
     * Size as Double
     */
    fun size() = size.toDouble()

    /**
     * Size of the given call as measured by this unit
     */
    suspend fun callSize(call: ApplicationCall): Double

    /**
     * Volume is measured in number of calls, with an optional call weighting function to give more weight to a call
     * based on any of it's properties
     */
    open class Calls(val callWeighting: ApplicationCall.() -> Double = { 1.0 }) : CallVolumeUnit {
        override val name = "calls"
        override val size: Int = 1
        override suspend fun callSize(call: ApplicationCall) = this.callWeighting(call)
    }

    /**
     * Volume is measured in number of bytes of request
     */
    @JvmInline
    value class Bytes(override val size: Int) : CallVolumeUnit {
        override val name: String
            get() = "bytes"

        override suspend fun callSize(call: ApplicationCall) = call.receive(ByteArray::class).size.toDouble()
    }
}
