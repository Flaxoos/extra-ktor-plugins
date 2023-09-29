package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.providers.RateLimitProvider
import io.github.flaxoos.ktor.server.plugins.ratelimiter.providers.RateLimiterResponse
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


class RateLimiter(
    val provider: () -> RateLimitProvider,

    val whiteListedHosts: Set<String> = emptySet(),
    val whiteListedPrincipals: Set<Principal> = emptySet(),
    val whiteListedAgents: Set<String> = emptySet(),
    val blackListedHosts: Set<String> = emptySet(),
    val blackListedPrincipals: Set<Principal> = emptySet(),
    val blackListedAgents: Set<String> = emptySet(),
    val blackListedCallerCallHandler: suspend (ApplicationCall) -> Unit = { call ->
        call.respond(HttpStatusCode.Forbidden)
    },
    val rateLimitExceededCallHandler: suspend ApplicationCall.(RateLimiterResponse.LimitedBy) -> Unit,
    val logRateLimitHits: Boolean = false,
    val loggerProvider: Application.() -> Logger = { log },
    application: Application
) {

    private val providers = mutableMapOf<Caller, Pair<ReentrantLock, RateLimitProvider>>()
    private val providersLock = reentrantLock()
    private val logger = loggerProvider(application)
    fun stop() =
        providersLock.withLock {
            providers.forEach {
                it.value.second.stop()
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
            val (bucketLock, provider) = providersLock.withLock {
                providers.getOrPut(call.extractCaller()) {
                    logger.debug("Putting new rate limiter for ${caller.toIdentifier()}")
                    reentrantLock() to provider()
                }
            }

            bucketLock.withLock {
                with(provider.tryAccept(call)) {
                    logger.debug(debugDetails(caller = caller, rateLimiterResponse = this))
                    if (this is RateLimiterResponse.LimitedBy) {
                        if (logRateLimitHits) logger.warn("$RATE_LIMIT_EXCEEDED_MESSAGE: $caller")
                        rateLimitExceededCallHandler.invoke(call, this)
                    }
                }
            }
        }
    }

    private fun debugDetails(
        caller: Caller,
        rateLimiterResponse: RateLimiterResponse,
    ) =
        "call from $caller ${if (rateLimiterResponse is RateLimiterResponse.LimitedBy) "" else "not"} limited ${
            if (rateLimiterResponse is RateLimiterResponse.LimitedBy) {
                rateLimiterResponse.message
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
