package io.flax.ktor.server.plugins

import io.ktor.server.application.PluginBuilder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.auth.AuthenticationChecked

/**
 * Unstable API, see [message] for explanation
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class KtorServerPluginUnstableAPI(val message: String)

/**
 * Application rate limiting plugin, apply to server to provide application scoped rate limiting,
 * see [RateLimitingConfiguration] for details on how to configure
 */
@KtorServerPluginUnstableAPI(
    message = "The authentication functionality of this has not been fully tested due to limitations in the ktor server test API\n" +
        "For now you can apply RouteRateLimiting to the base route"
)
val ApplicationRateLimiting = createApplicationPlugin(
    name = "ApplicationRateLimiting",
    createConfiguration = ::RateLimitingConfiguration
) { applyNewRateLimiter() }

/**
 * Route rate limiting plugin, apply to route to provide route scoped rate limiting,
 * see [RateLimitingConfiguration] for details on how to configure
 */
val RouteRateLimiting = createRouteScopedPlugin(
    name = "RouteRateLimiting",
    createConfiguration = ::RateLimitingConfiguration
) { applyNewRateLimiter() }

private fun PluginBuilder<RateLimitingConfiguration>.applyNewRateLimiter() {
    val rateLimiter = RateLimiter(
        limit = pluginConfig.limit,
        timeWindow = pluginConfig.timeWindow,
        whiteListedHosts = pluginConfig.whiteListedHosts,
        whiteListedPrincipals = pluginConfig.whiteListedPrincipals,
        whiteListedAgents = pluginConfig.whiteListedAgents,
        blackListedHosts = pluginConfig.blackListedHosts,
        blackListedPrincipals = pluginConfig.blackListedPrincipals,
        blackListedAgents = pluginConfig.blackListedAgents,
        blackListedCallerCallHandler = pluginConfig.blackListedCallerCallHandler,
        burstLimit = pluginConfig.burstLimit,
        rateLimitExceededCallHandler = pluginConfig.rateLimitExceededCallHandler,
        logRateLimitHits = pluginConfig.logRateLimitHits,
        loggerProvider = pluginConfig.loggerProvider,
        application = application
    )

    on(AuthenticationChecked) { call ->
        rateLimiter.handleCall(call)
    }

    on(CallFailed) { call, _ ->
        rateLimiter.handleCallFailure(call)
    }

    onCallRespond { call, _ ->
        rateLimiter.handleCallResponse(call)
    }
}
