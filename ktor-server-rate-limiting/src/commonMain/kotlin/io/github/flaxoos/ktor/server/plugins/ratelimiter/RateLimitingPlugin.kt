package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.origin
import io.ktor.server.request.userAgent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate limiting plugin, apply to route to provide route scoped rate limiting,
 * see [RateLimitingConfiguration] for details on how to configure
 */
val RateLimiting = createRouteScopedPlugin(
    name = "RateLimiting",
    createConfiguration = ::RateLimitingConfiguration
) { applyNewRateLimiter() }

private fun PluginBuilder<RateLimitingConfiguration>.applyNewRateLimiter() {
    val rateLimiters = mutableMapOf<Caller, RateLimiter>()
    val rateLimitersLock = Mutex()

    if (pluginConfig.rateLimiterConfiguration.callVolumeUnit is CallVolumeUnit.Bytes) {
        this.application.install(DoubleReceive) {
            this@applyNewRateLimiter.application.log.info("Installing double receive plugin because call volume unit is bytes")
            this.cacheRawRequest = true
        }
    }

    on(AuthenticationChecked) { call ->
        with(pluginConfig) {
            val caller = call.extractCaller()
            application.log.debug("Handling call by ${caller.toIdentifier()}")

            if (caller.remoteHost in blackListedHosts || caller.principal in blackListedPrincipals || caller.userAgent in blackListedAgents) {
                application.log.debug(
                    "User ${caller.toIdentifier()} is blacklisted by ${
                    listOfNotNull(
                        if (caller.remoteHost in blackListedHosts) "host" else null,
                        if (caller.principal in blackListedPrincipals) "principal" else null,
                        if (caller.userAgent in blackListedAgents) "user agent" else null
                    ).joinToString(",")
                    }"
                )
                blackListedCallerCallHandler(call)
                return@with
            }
            if (caller.remoteHost in whiteListedHosts || caller.principal in whiteListedPrincipals || caller.userAgent in whiteListedAgents) {
                application.log.debug(
                    "User ${caller.toIdentifier()} is whitelisted by ${
                    listOfNotNull(
                        if (caller.remoteHost in whiteListedHosts) "host" else null,
                        if (caller.principal in whiteListedPrincipals) "principal" else null,
                        if (caller.userAgent in whiteListedAgents) "user agent" else null
                    ).joinToString(",")
                    }"
                )
                return@with
            }

            val provider = rateLimitersLock.withLock {
                rateLimiters.getOrPut(call.extractCaller()) {
                    application.log.debug("Putting new rate limiter for ${caller.toIdentifier()}")
                    rateLimiterConfiguration.provideRateLimiter(application = application).invoke()
                }
            }

            with(provider.tryAccept(call)) {
                application.log.debug(debugDetails(caller = caller))
                when (this) {
                    is RateLimiterResponse.LimitedBy -> {
                        application.log.debug("$RATE_LIMIT_EXCEEDED_MESSAGE: $caller")
                        rateLimitExceededHandler.invoke(call, this)
                    }

                    is RateLimiterResponse.NotLimited -> {
                        application.log.debug("Call accepted: $caller")
                        callAcceptedHandler.invoke(call, this)
                    }
                }
            }
        }
    }
}

private fun RateLimiterResponse.debugDetails(
    caller: Caller
) =
    "call from $caller ${if (this is RateLimiterResponse.LimitedBy) "" else "not"} limited ${
    if (this is RateLimiterResponse.LimitedBy) {
        this.message
    } else {
        ""
    }
    }"

private fun ApplicationCall.extractCaller(): Caller {
    val remoteHost = this.request.origin.remoteHost
    val userAgent = this.request.userAgent()
    val principal = this.principal<Principal>().also {
        if (it == null) {
            application.log.debug(
                "No authenticated principal found in call, identification is based on http headers X-Forwarded-For and User-Agent"
            )
        }
    }
    return Caller(remoteHost, userAgent, principal)
}

private data class Caller(
    val remoteHost: String,
    val userAgent: String?,
    val principal: Principal?
) {
    fun toIdentifier() = "$remoteHost|${userAgent ?: ""}|${principal ?: ""}"
    override fun toString() = toIdentifier()
}
