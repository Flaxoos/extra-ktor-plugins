package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.github.flaxoos.ktor.server.plugins.ratelimiter.providers.RateLimiterResponse.LimitedBy
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.utils.io.CancellationException
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Leaky Bucket, allows for constant rate of delivery,
 * Fair distribution between clients, but not ideal for handling bursts
 *
 * The configured [rate] will be the rate in which requests will leak from the bucket, when full.
 * This means the call will be suspended until it leaks out of the bucket, at which point the [tryAccept] function would
 * return. No timeout is set, so the call can be suspended indefinitely in theory, and it is up to the server or cliuent
 * duration, dividing the added volume between the updates, and given a [rate] greater than 1, preventing request bursts,
 * leading to a constant rate of requests
 *
 */
class LeakyBucket(
    override val coroutineScope: CoroutineScope,
    override val log: KLogger = logger,
    override val rate: Duration,
    override val capacity: Int,
    private val queuePeekRate: Duration = 1.milliseconds,
    callWeighting: ApplicationCall.() -> Double = { 1.0 }
) : Bucket() {
    override val callVolumeUnit: CallVolumeUnit = CallVolumeUnit.Calls(callWeighting)
    private val channel =
        Channel<CallLeak>(capacity = capacity, onBufferOverflow = BufferOverflow.DROP_LATEST, onUndeliveredElement = {
            log.debug { "Channel is full" }
            throw Exception("Channel is full")
        })

    init { volumeChangeJob.start() }

    override suspend fun changeVolume() {
        channel.tryReceive().let { result ->
            when {
                result.isSuccess -> {
                    result.getOrThrow().let { callLeak -> callLeak.leaked.update { true } }
                    log.trace { "Bucket leaked" }
                }

                result.isFailure -> log.trace { "Bucket empty" }

                else -> log.error { "Bucket closed" }
            }
        }
    }

    override suspend fun tryAccept(call: ApplicationCall): RateLimiterResponse {
        log.debug { "Trying to add call ${call.callAttributes()} to bucket" }
        val callLeak = CallLeak(call)
        runCatching {
            channel.send(callLeak)
            log.debug { "Call ${call.callAttributes()} added to bucket" }
        }.getOrElse {
            log.debug { "Call ${call.callAttributes()} rejected due to bucket overflow" }
            return LimitedBy(
                this,
                resetIn = rate,
                exceededBy = 1,
                message = "Bucket of size $capacity is full, call rejected"
            )
        }
        return checkIfLeaked(callLeak)
    }

    private suspend fun checkIfLeaked(callLeak: CallLeak): RateLimiterResponse =
        runCatching {
            withTimeoutOrNull(rate * capacity + queuePeekRate) {
                while (isActive) {
                    if (callLeak.leaked.value) break
                    delay(queuePeekRate)
                }
                log.debug { "Call ${callLeak.call.callAttributes()} leaked" }
                RateLimiterResponse.NotLimited(
                    this@LeakyBucket,
                    remaining = null
                )
            } ?: error("Call added to bucket but never leaked")
        }.recoverCatching {
            if (it is CancellationException) {
                RateLimiterResponse.RequestCancelled(this)
            } else
                throw it
        }.getOrThrow()

    private fun ApplicationCall.callAttributes() =
        attributes.allKeys.joinToString("\n") { "${it.name}: ${attributes[it]}" }
}

internal class CallLeak(internal val call: ApplicationCall) {
    internal val leaked = atomic(false)
}