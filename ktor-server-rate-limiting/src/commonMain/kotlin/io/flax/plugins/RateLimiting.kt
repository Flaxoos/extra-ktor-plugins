package io.flax.plugins

import io.flax.plugins.Caller.Companion.extractCaller
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
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
import kotlin.time.Duration

private const val RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded"

/**
 * Rate limit plugin configuration.
 *
 * Be careful using whitelisting, as the caller can abuse it by overriding the host or
 * user-agent by manipulating the headers, it is safest to use [Principal] whitelisting,
 * as it relies on authentication.
 */
class RateLimitConfiguration {
    /**
     * How many requests per time window
     */
    var limit: Int = Int.MAX_VALUE

    /**
     * The time window in which the rate limit is applied
     */
    var timeWindow: Duration = Duration.INFINITE

    /**
     * Any Hosts that are whitelisted, i.e. will be allowed through without rate limiting
     */
    var whiteListedHosts: Set<String> = emptySet()

    /**
     * Any [Principal]s that are whitelisted, i.e. will be allowed through without rate limiting
     */
    var whiteListedPrincipals: Set<Principal> = emptySet()

    /**
     * Any user-agents that are whitelisted, i.e. will be allowed through without rate limiting
     */
    var whiteListedAgents: Set<String> = emptySet()

    /**
     * Any Hosts that are blacklisted, i.e. will not be allowed through in any case, handled by [blackListedCallerCallHandler]
     */
    var blackListedHosts: Set<String> = emptySet()

    /**
     * Any [Principal]s that are blacklisted, i.e. will not be allowed through in any case, handled by [blackListedCallerCallHandler]
     */
    var blackListedPrincipals: Set<Principal> = emptySet()

    /**
     * Any user-agents that are blacklisted, i.e. will not be allowed through in any case, handled by [blackListedCallerCallHandler]
     */
    var blackListedAgents: Set<String> = emptySet()

    /**
     * The call handler for blacklisted Callers, use to define the response for blacklisted Callers, default is respond with 403
     */
    var blackListedCallerCallHandler: suspend (ApplicationCall) -> Unit = { call ->
        call.respond(HttpStatusCode.Forbidden)
    }

    /**
     * Allow a burst of requests to be processed before the limit kicks in
     */
    var burstLimit: Int = limit

    /**
     * The call handler for rate limited IPs, use to define the response for rate limited IPs, default is respond with 429
     */
    var rateLimitExceededCallHandler: suspend (ApplicationCall, Int) -> Unit = { call, count ->
        call.respond(HttpStatusCode.TooManyRequests, "$RATE_LIMIT_EXCEEDED_MESSAGE: call count: $count, limit: $limit")
    }

    /**
     * Should log rate limit hits
     */
    var logRateLimitHits: Boolean = false

    /**
     * Logger provider to use for logging by the plugin
     */
    var loggerProvider: suspend (ApplicationCall) -> Logger = { call -> call.application.log }

    /**
     * Configuration for the plugin
     */
    var dynamicConfig: suspend () -> RateLimitConfiguration = { this }

    init {
        check(burstLimit > 0) {
            "burstLimit must be > 0"
        }
    }
}

/**
 * Rate limit plugin, apply to route to provide rate limiting for it,
 * see [RateLimitConfiguration] for details on how to configure
 */
val RateLimitingPlugin = createRouteScopedPlugin(
    name = "RateLimitingPlugin",
    createConfiguration = ::RateLimitConfiguration
) {
    val callStore = mutableMapOf<Caller, Pair<ReentrantLock, CallCount>>()
    val callStoreLock = reentrantLock()

    on(AuthenticationChecked) { call ->
        with(pluginConfig.dynamicConfig.invoke()) {
            val caller = call.extractCaller()

            if (caller.remoteHost in blackListedHosts ||
                caller.principal in blackListedPrincipals ||
                caller.userAgent in blackListedAgents
            ) {
                blackListedCallerCallHandler(call)
                return@on
            }
            val logger = loggerProvider(call)

            if (caller.remoteHost !in whiteListedHosts &&
                caller.principal !in whiteListedPrincipals &&
                caller.userAgent !in whiteListedAgents
            ) {
                val (ipCallCountLock, ipCallCount) =
                    callStoreLock.withLock {
                        callStore.getOrPut(call.extractCaller()) {
                            logger.debug("Putting new call count for call by ${caller.toIdentifier()}")
                            reentrantLock() to CallCount()
                        }
                    }

                ipCallCountLock.withLock {
                    with(ipCallCount) {
                        val isBurst =
                            (now().epochSeconds - lastResetTime) <= (timeWindow.inWholeMilliseconds * (limit / burstLimit))

                        if ((isBurst && count >= burstLimit) || (!isBurst && count >= limit)) {
                            if (logRateLimitHits) {
                                logger.warn("$RATE_LIMIT_EXCEEDED_MESSAGE: $caller")
                            }
                            logger.debug(
                                debugDetails(
                                    caller = caller,
                                    rateLimitConfiguration = this@createRouteScopedPlugin.pluginConfig,
                                    callCount = this,
                                    isBurst = isBurst,
                                    limited = true
                                )
                            )
                            rateLimitExceededCallHandler.invoke(call, count)
                        } else {
                            logger.debug(
                                debugDetails(
                                    caller = caller,
                                    rateLimitConfiguration = this@createRouteScopedPlugin.pluginConfig,
                                    callCount = this,
                                    isBurst = isBurst
                                )
                            )
                            count++
                        }
                    }
                }
            }
        }
    }

    on(CallFailed) { call, _ ->
        callStore.release(callStoreLock, call)
    }

    onCallRespond { call, _ ->
        callStore.release(callStoreLock, call)
    }

    this.application.launch {
        while (this@createRouteScopedPlugin.application.isActive) {
            delay(pluginConfig.timeWindow)
            this@createRouteScopedPlugin.application.log.debug("Clearing caller call count")
            callStoreLock.withLock { callStore.clear() }
        }
    }
}

private fun CallStore.release(
    lock: ReentrantLock,
    call: ApplicationCall
) {
    val (ipCallCountLock, ipCallCount) = lock.withLock {
        get(call.extractCaller()) ?: return
    }
    ipCallCountLock.withLock {
        ipCallCount.count--
        ipCallCount.lastResetTime = now().toEpochMilliseconds()
    }
}

private fun debugDetails(
    caller: Caller,
    rateLimitConfiguration: RateLimitConfiguration,
    callCount: CallCount,
    isBurst: Boolean,
    limited: Boolean = false
) =
    "call from $caller ${if (limited) "" else "not"} limited, limit: ${rateLimitConfiguration.limit}, count: ${callCount.count}, " +
        "last reset time: ${callCount.lastResetTime}, is burst: $isBurst"

private data class CallCount(
    var count: Int = 0,
    var lastResetTime: Long = now().toEpochMilliseconds()
)

private data class Caller(
    val remoteHost: String,
    val userAgent: String?,
    val principal: Principal?
) {
    companion object {
        fun ApplicationCall.extractCaller(): Caller {
            val remoteHost = this.request.origin.remoteHost
            val userAgent = this.request.userAgent()
            val principal = this.principal<Principal>()
                .also {
                    if (it == null) {
                        this.application.log.debug(
                            "No authenticated principal found in call, identification " +
                                "is based on http headers X-Forwarded-For and User-Agent"
                        )
                    }
                }
            return Caller(remoteHost, userAgent, principal)
        }
    }

    fun toIdentifier() = "$remoteHost|${userAgent ?: ""}|${principal ?: ""}"
    override fun toString() = toIdentifier()
}

private typealias CallStore = Map<Caller, Pair<ReentrantLock, CallCount>>
