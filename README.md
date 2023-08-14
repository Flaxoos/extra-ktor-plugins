# Ktor Server Multiplatform Plugins Library
[![Build Workflow](https://github.com/idoflax/flax-ktor-plugins/workflows/Build/badge.svg)](https://github.com/idoflax/flax-ktor-plugins/actions)
https://gist.github.com/idoflax/2e4bf8a120b7dd025243ba081ee72bb6
This project provides a suite of useful, efficient, and customizable plugins for your Ktor Server, crafted in Kotlin, available for multiplatform. Whether you need rate limiting, logging, monitoring, or more, you can enhance your server's capabilities with these plugins.

## Features

These plugins offer a wide range of features designed to provide additional layers of control, security, and utility to your server. Here are some key features:

* **Rate Limiting Plugin**: Limit the number of requests a client can make in a specific time window. This plugin also offers whitelist and blacklist features for hosts, principals, and user-agents. You can configure custom responses for blacklisted callers or when the rate limit is exceeded.

* **Coming Soon**: Stay tuned for additional plugins that will further extend the capabilities of your Ktor server.

## Usage

You can apply and configure each plugin to suit your needs. Here's a basic example of using the Rate Limiting Plugin:

```kotlin
install(RateLimitingPlugin) {
    limit = 100
    timeWindow = Duration.ofMinutes(1)
    burstLimit = 10
    whiteListedHosts = setOf("trusted-host.com")
    blackListedAgents = setOf("malicious-agent")
    rateLimitExceededCallHandler = { call, count ->
        call.respond(HttpStatusCode.TooManyRequests, "You have exceeded the limit of requests. Limit: 100, Your calls: $count")
    }
    logRateLimitHits = true
}
```

Detailed usage is available in the tests and in [examples repository](https://github.com/idoflax/flax-ktor-plugins/ktor-plugins-examples)

## Installation

This library is available via a JAR file that can be included in your Kotlin or Java project, or as a library to be included in your Gradle/Maven project.

## Contributing

Contributions are always welcome! If you have an idea for a plugin or want to improve an existing one, feel free to fork this repository and submit a pull request.
