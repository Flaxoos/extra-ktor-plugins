package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.Bucket
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketCapacityUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketResponse
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketType
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.TimeWindow
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.userAgent
import io.ktor.server.response.respond
import io.ktor.util.logging.Logger
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.time.Duration


class RateLimiter(
    val bucketType: BucketType,
    val capacityUnit: BucketCapacityUnit = BucketCapacityUnit.Calls(),
    val capacity: Double,
    val volumeChangeRate: Pair<Duration, Double>,
    val timeWindow: TimeWindow? = null,

    val whiteListedHosts: Set<String> = emptySet(),
    val whiteListedPrincipals: Set<Principal> = emptySet(),
    val whiteListedAgents: Set<String> = emptySet(),
    val blackListedHosts: Set<String> = emptySet(),
    val blackListedPrincipals: Set<Principal> = emptySet(),
    val blackListedAgents: Set<String> = emptySet(),
    val blackListedCallerCallHandler: suspend (ApplicationCall) -> Unit = { call ->
        call.respond(HttpStatusCode.Forbidden)
    },
    val rateLimitExceededCallHandler: suspend ApplicationCall.(BucketResponse.LimitedBy) -> Unit,
    val logRateLimitHits: Boolean = false,
    val loggerProvider: Application.() -> Logger = { log },
    application: Application
) {

    private val buckets = mutableMapOf<Caller, Pair<ReentrantLock, Bucket>>()
    private val bucketsLock = reentrantLock()
    private val logger = loggerProvider(application)

    suspend fun handleCall(call: ApplicationCall) {
        val caller = call.extractCaller()
        logger.debug("Handling call by ${caller.toIdentifier()}")

        if (caller.remoteHost in blackListedHosts || caller.principal in blackListedPrincipals || caller.userAgent in blackListedAgents) {
            blackListedCallerCallHandler(call)
            return
        }

        if (caller.remoteHost !in whiteListedHosts && caller.principal !in whiteListedPrincipals && caller.userAgent !in whiteListedAgents) {
            val (bucketLock, bucket) = bucketsLock.withLock {
                buckets.getOrPut(call.extractCaller()) {
                    logger.debug("Putting new bucket for ${caller.toIdentifier()}")
                    reentrantLock() to Bucket(
                        volumeChangeRate = volumeChangeRate,
                        capacity = capacity,
                        capacityUnit = capacityUnit,
                        timeWindow = timeWindow,
                        type = bucketType,
                        volumeUpdateScope = call.application,
                    )
                }
            }

            bucketLock.withLock {
                when (val bucketResponse = bucket.handleCall(call)) {
                    is BucketResponse.NotLimited -> logger.debug(
                        debugDetails(
                            caller = caller,
                            bucket = bucket,
                            bucketResponse = bucketResponse
                        )
                    )

                    is BucketResponse.LimitedBy -> {
                        if (logRateLimitHits) {
                            logger.warn("$RATE_LIMIT_EXCEEDED_MESSAGE: $caller")
                        }
                        logger.debug(debugDetails(caller = caller, bucket = bucket, bucketResponse = bucketResponse))
                        rateLimitExceededCallHandler.invoke(call, bucketResponse)
                    }
                }
            }
        }
    }

    private fun debugDetails(
        caller: Caller,
        bucket: Bucket,
        bucketResponse: BucketResponse,
    ) =
        "call from $caller ${if (bucketResponse is BucketResponse.LimitedBy) "" else "not"} limited ${
            if (bucketResponse is BucketResponse.LimitedBy) {
                bucketResponse.message
            } else ""
        }"

    private fun ApplicationCall.extractCaller(): Caller {
        val remoteHost = this.request.origin.remoteHost
        val userAgent = this.request.userAgent()
        val principal = this.principal<Principal>().also {
            if (it == null) {
                logger.debug(
                    "No authenticated principal found in call, identification " + "is based on http headers X-Forwarded-For and User-Agent"
                )
            }
        }
        return Caller(remoteHost, userAgent, principal)
    }
}

internal data class Caller(
    val remoteHost: String,
    val userAgent: String?,
    val principal: Principal?
) {
    fun toIdentifier() = "$remoteHost|${userAgent ?: ""}|${principal ?: ""}"
    override fun toString() = toIdentifier()
}
