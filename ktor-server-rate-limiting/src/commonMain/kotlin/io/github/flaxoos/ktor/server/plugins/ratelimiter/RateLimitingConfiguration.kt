package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketCapacityUnit
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketResponse
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.BucketType
import io.github.flaxoos.ktor.server.plugins.ratelimiter.buckets.TimeWindow
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.Principal
import io.ktor.server.response.respond
import io.ktor.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded"
internal const val X_RATE_LIMIT = "X-RateLimit"

/**
 * Rate limit plugin configuration.
 *
 * Be careful using whitelisting, as the caller can abuse it by overriding the host or
 * user-agent by manipulating the headers, it is safest to use [Principal] whitelisting,
 * as it relies on authentication.
 */
class RateLimitingConfiguration {
    /**
     * What type of bucket to use for the rate limiter, defaults to [BucketType.TOKEN]
     */
    var bucketType: BucketType = BucketType.Token

    /**
     * What should the bucket capacity be measure in, defaults to [BucketCapacityUnit.Calls]
     */
    var capacityUnit: BucketCapacityUnit = BucketCapacityUnit.Calls()

    /**
     * Refill/Empty rate for the Token/Leaky bucket
     */
    var volumeChangeRate: Pair<Duration, Double> = 1.seconds to 1.0

    /**
     * Bucket capacity, measured in the configured [capacityUnit]
     */
    var capacity: Double = Double.MAX_VALUE

    /**
     * An optional time window constraint
     */
    var timeWindow: TimeWindow? = null

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
     * The call handler for rate limited IPs, use to define the response for rate limited IPs, default is respond with 429 and appropriate X-RateLimit headers
     */
    val rateLimitExceededCallHandler: suspend ApplicationCall.(BucketResponse.LimitedBy) -> Unit = { limitedBy ->
        respond(
            HttpStatusCode.TooManyRequests, "$RATE_LIMIT_EXCEEDED_MESSAGE: ${limitedBy.message}"
        )
        response.headers.append("$X_RATE_LIMIT-Limit", "$capacity ${capacityUnit.measures}")
//        response.headers.append("$X_RATE_LIMIT-Remaining", "${TODO()} ${tokenType.measures}")
        response.headers.append("$X_RATE_LIMIT-Reset", "${limitedBy.resetIn.inWholeMilliseconds}")
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
        require(capacity > 0) {
            "capacity must be > 0"
        }
    }
}
