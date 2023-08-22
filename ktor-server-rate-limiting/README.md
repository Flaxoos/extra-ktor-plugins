# Rate Limiting Plugin for Ktor Server

<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-server-rate-limiting/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/96.04-green?logo=kotlin&label=koverage&style=flat)</a>

The `RateLimitingPlugin` is a utility designed to limit the number of requests a client can make in a specific time window to your Ktor server. It offers the flexibility of whitelisting and blacklisting based on hosts, user-agents, and authentication principals.

## Features:

- **Configurable Limit**: Set a limit on the number of requests within a specified time window.

- **Burst Handling**: Allows a burst of requests to be processed before the limit kicks in.

- **Whitelist & Blacklist**:
    - Whitelist or blacklist based on the client's host, user-agent, or principal.
    - Default response status for blacklisted callers is `403 Forbidden`.

- **Customizable Response**: Set your custom response when the rate limit is exceeded. The default response status is `429 Too Many Requests`.

- **Logging**: Log rate limit hits for better monitoring and debugging.

## How to Use:

To apply the `RateLimitingPlugin`, you need to `install` it in your Ktor route and configure as per your requirements:

```kotlin
routing {
    route("limited-route") {
        limit = 100
        timeWindow = 1.minutes
        burstLimit = 10
        whiteListedHosts = setOf("trusted-host.com")
        blackListedAgents = setOf("malicious-agent")
        rateLimitExceededCallHandler = { call, count ->
            call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded: call count: $count, limit: $limit")
        }
        logRateLimitHits = true
    }
}
```

## Configuration Options:

The following are the configurable parameters:

- `limit`: Number of allowed requests within the `timeWindow`.

- `timeWindow`: The duration in which the limit is applied.

- `burstLimit`: Number of requests to allow as a burst before applying rate limits.

- `whiteListedHosts`: Hosts that are always allowed.

- `blackListedHosts`: Hosts that are always blocked.

- `whiteListedPrincipals`: Authenticated principals that are always allowed.

- `blackListedPrincipals`: Authenticated principals that are always blocked.

- `whiteListedAgents`: User-agents that are always allowed.

- `blackListedAgents`: User-agents that are always blocked.

- `blackListedCallerCallHandler`: Custom response handler for blacklisted callers.

- `rateLimitExceededCallHandler`: Custom response handler for rate limited IPs.

- `logRateLimitHits`: Whether to log rate limit hits or not.

- `loggerProvider`: Logger provider for logging from within the plugin, default is the `ApplicationCall` logger.

For a comprehensive overview of all configuration options, see the `RateLimitConfiguration` class.
