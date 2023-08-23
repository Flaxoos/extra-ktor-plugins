package io.flax.ktor.server.plugins

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RateLimiter(
    val limit: Int = Int.MAX_VALUE,
    val timeWindow: Duration = Duration.INFINITE,
    val whiteListedHosts: Set<String> = emptySet(),
    val whiteListedPrincipals: Set<Principal> = emptySet(),
    val whiteListedAgents: Set<String> = emptySet(),
    val blackListedHosts: Set<String> = emptySet(),
    val blackListedPrincipals: Set<Principal> = emptySet(),
    val blackListedAgents: Set<String> = emptySet(),
    val blackListedCallerCallHandler: suspend (ApplicationCall) -> Unit = { call ->
        call.respond(HttpStatusCode.Forbidden)
    },
    val burstLimit: Int = limit,
    val rateLimitExceededCallHandler: suspend (ApplicationCall, Int) -> Unit = { call, count ->
        call.respond(HttpStatusCode.TooManyRequests, "$RATE_LIMIT_EXCEEDED_MESSAGE: call count: $count, limit: $limit")
    },
    val logRateLimitHits: Boolean = false,
    val loggerProvider: Application.() -> Logger = { log },
    application: Application
) {

    private val callStore = mutableMapOf<Caller, Pair<ReentrantLock, CallCount>>()
    private val callStoreLock = reentrantLock()
    private val logger = loggerProvider(application)

    init {
        application.launch {
            while (application.isActive) {
                delay(timeWindow)
                logger.debug("Clearing caller call count")
                callStoreLock.withLock { callStore.clear() }
            }
        }
    }

    suspend fun handleCall(call: ApplicationCall) {
        val caller = call.extractCaller()
        logger.debug("Handling call by ${caller.toIdentifier()}")

        if (caller.remoteHost in blackListedHosts || caller.principal in blackListedPrincipals || caller.userAgent in blackListedAgents) {
            blackListedCallerCallHandler(call)
            return
        }

        if (caller.remoteHost !in whiteListedHosts && caller.principal !in whiteListedPrincipals && caller.userAgent !in whiteListedAgents) {
            val (ipCallCountLock, ipCallCount) = callStoreLock.withLock {
                callStore.getOrPut(call.extractCaller()) {
                    logger.debug("Putting new call count for call by ${caller.toIdentifier()}")
                    reentrantLock() to CallCount()
                }
            }

            ipCallCountLock.withLock {
                with(ipCallCount) {
                    val isBurst =
                        (now().toEpochMilliseconds() - lastResetTime.toEpochMilliseconds()) <= (timeWindow.inWholeMilliseconds * (limit.milliseconds / burstLimit.milliseconds))

                    if ((isBurst && count >= burstLimit) || (!isBurst && count >= limit)) {
                        if (logRateLimitHits) {
                            logger.warn("$RATE_LIMIT_EXCEEDED_MESSAGE: $caller")
                        }
                        logger.debug(debugDetails(caller = caller, callCount = this, isBurst = isBurst, limited = true))
                        rateLimitExceededCallHandler.invoke(call, count)
                    } else {
                        logger.debug(debugDetails(caller = caller, callCount = this, isBurst = isBurst))
                        count++
                    }
                }
            }
        }
    }

    internal fun handleCallFailure(call: ApplicationCall) {
        callStore.release(callStoreLock, call)
    }

    internal fun handleCallResponse(call: ApplicationCall) {
        callStore.release(callStoreLock, call)
    }

    private fun CallStore.release(lock: ReentrantLock, call: ApplicationCall) {
        val (ipCallCountLock, ipCallCount) = lock.withLock {
            get(call.extractCaller()) ?: return
        }
        ipCallCountLock.withLock {
            ipCallCount.count--
            ipCallCount.lastResetTime = now()
        }
    }

    private fun debugDetails(
        caller: Caller,
        callCount: CallCount,
        isBurst: Boolean,
        limited: Boolean = false
    ) =
        "call from $caller ${if (limited) "" else "not"} limited, limit: $limit, count: ${callCount.count}, " + "last reset time: ${callCount.lastResetTime}, is burst: $isBurst"

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

internal class CallCount {
    var count: Int = 0
    var lastResetTime: Instant = now()
}

internal data class Caller(
    val remoteHost: String,
    val userAgent: String?,
    val principal: Principal?
) {
    fun toIdentifier() = "$remoteHost|${userAgent ?: ""}|${principal ?: ""}"
    override fun toString() = toIdentifier()
}

internal typealias CallStore = Map<Caller, Pair<ReentrantLock, CallCount>>
