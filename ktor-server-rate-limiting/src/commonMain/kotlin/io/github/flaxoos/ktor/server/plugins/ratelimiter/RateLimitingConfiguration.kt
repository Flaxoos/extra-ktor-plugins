package io.github.flaxoos.ktor.server.plugins.ratelimiter

import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.LeakyBucket
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.SlidingWindow
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.Principal
import io.ktor.server.response.respond
import kotlinx.datetime.Clock.System.now
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE

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
     * Configuration for the rate limiter
     */
    var rateLimiterConfiguration: RateLimiterConfiguration = RateLimiterConfiguration()

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
    var blackListedCallerCallHandler: suspend RateLimitingConfiguration.(ApplicationCall) -> Unit = { call ->
        call.respond(HttpStatusCode.Forbidden)
    }

    /**
     * The call handler for accepted calls, use to define the response for accepted calls, by default, adds appropriate X-RateLimit headers
     */
    val callAcceptedHandler: suspend ApplicationCall.(RateLimiterResponse.NotLimited) -> Unit = {
        response.headers.append("$X_RATE_LIMIT-Remaining", "${it.remaining}")
        response.headers.append("$X_RATE_LIMIT-Measured-by", it.rateLimiter.callVolumeUnit.name)
    }

    /**
     * The call handler for rate limited IPs, use to define the response for rate limited IPs, default is respond with 429 and appropriate X-RateLimit headers
     */
    val rateLimitExceededHandler: suspend ApplicationCall.(RateLimiterResponse.LimitedBy) -> Unit =
        { rateLimiterResponse ->
            respond(HttpStatusCode.TooManyRequests, "$RATE_LIMIT_EXCEEDED_MESSAGE: ${rateLimiterResponse.message}")
            this.response.headers.append("$X_RATE_LIMIT-Limit", "${rateLimiterResponse.rateLimiter.capacity}")
            this.response.headers.append("$X_RATE_LIMIT-Measured-by", rateLimiterResponse.rateLimiter.callVolumeUnit.name)
            this.response.headers.append("$X_RATE_LIMIT-Reset", "${rateLimiterResponse.resetIn.inWholeMilliseconds}")
        }

    class RateLimiterConfiguration(
        /**
         * The rate limiter implementation
         */
        var type: KClass<out RateLimiter> = TokenBucket::class,
        /**
         * The rate limiter rate
         */
        var rate: Duration = INFINITE,
        /**
         * The rate limiter capacity
         */
        var capacity: Int = Int.MAX_VALUE,
        /**
         * A time provider in milliseconds
         */
        var clock: () -> Long = { now().toEpochMilliseconds() },
        /**
         * The unit by which the rate limiter capacity is measured, not applicable for [LeakyBucket]
         */
        var callVolumeUnit: CallVolumeUnit = CallVolumeUnit.Calls()
    ) {
        init {
            require(capacity > 0) {
                "capacity must be > 0"
            }
        }

        fun provideRateLimiter(application: Application): () -> RateLimiter = when (type) {
            LeakyBucket::class -> {
                when (callVolumeUnit) {
                    is CallVolumeUnit.Bytes -> {
                        application.log.warn(
                            "LeakyBucket does not support CallVolumeUnit.Bytes, " +
                                    "will use CallVolumeUnit.Calls"
                        )
                    }

                    is CallVolumeUnit.Calls -> if (callVolumeUnit.size.compareTo(1) != 0) {
                        application.log.warn(
                            "LeakyBucket does not support CallVolumeUnit.Calls with size " +
                                    "!= 1, 1 will be effectively used"
                        )
                    }
                }

                {
                    LeakyBucket(
                        rate = rate,
                        capacity = capacity,
                        clock = clock
                    )
                }

            }

            SlidingWindow::class -> (
                    {
                        SlidingWindow(
                            rate = rate,
                            capacity = capacity,
                            callVolumeUnit = callVolumeUnit,
                            clock = clock
                        )
                    }
                    )

            TokenBucket::class -> (
                    {
                        TokenBucket(
                            rate = rate,
                            capacity = capacity,
                            callVolumeUnit = callVolumeUnit,
                            clock = clock
                        )
                    }
                    )

            else -> {
                error("Unsupported provider type: $type")
            }
        }
    }
}
