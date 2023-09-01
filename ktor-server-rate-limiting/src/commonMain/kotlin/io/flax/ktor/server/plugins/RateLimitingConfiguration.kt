package io.flax.ktor.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.Principal
import io.ktor.server.response.respond
import io.ktor.util.logging.Logger
import kotlin.time.Duration

internal const val RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded"

/**
 * Rate limit plugin configuration.
 *
 * Be careful using whitelisting, as the caller can abuse it by overriding the host or
 * user-agent by manipulating the headers, it is safest to use [Principal] whitelisting,
 * as it relies on authentication.
 */
class RateLimitingConfiguration {
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
    var loggerProvider: Application.() -> Logger = { log }

    init {
        check(burstLimit > 0) {
            "burstLimit must be > 0"
        }
    }
}
