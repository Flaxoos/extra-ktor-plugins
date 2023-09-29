package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.ktor.server.application.ApplicationCall
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlin.time.Duration

sealed interface RateLimitProvider {

    /**
     * Desired change over time
     */
    val rate: Duration

    /**
     * The maximum capacity, as measured in [CallVolumeUnit.Calls] or [CallVolumeUnit.Bytes]
     */
    val capacity: Int

    /**
     * In what unit are calls measured
     */
    val callVolumeUnit: CallVolumeUnit

    /**
     * Try to accept a call
     *
     * @param call the call to accept
     * @return response detailing if the call was accepted and details in the case of rejection
     */
    suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse

    fun stop()
}

sealed interface RateLimiterResponse {
    val provider: RateLimitProvider


    data class NotLimited(
        override val provider: RateLimitProvider,
        val remaining: Number? = null,
    ) : RateLimiterResponse

    data class LimitedBy(
        override val provider: RateLimitProvider,
        val exceededBy: Number,
        val resetIn: Duration,
        val message: String,
    ) : RateLimiterResponse


    data class RequestCancelled(
        override val provider: RateLimitProvider
    ) : RateLimiterResponse


}
