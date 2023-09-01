# Circuit Breaker Plugin for Ktor Client
![Language](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin)
<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-client-circuit-breaker/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/93.58-green?logo=kotlin&label=koverage&style=flat)</a>

The Circuit Breaker pattern is a crucial part of modern software architecture, allowing you to gracefully handle potential failures when communicating with external services. The Ktor Circuit Breaker plugin is an implementation of this pattern, providing you with an easy way to incorporate this mechanism into your Ktor clients.

## Features:

- **Flexible Configuration**: Define both global and specific circuit breaker configurations.

- **Support for Named Circuit Breakers**: Allows for specific configurations for different endpoints or services.

- **Tight Integration with Ktor Client**: Uses familiar Ktor constructs.

## How to Use:

You can configure the Circuit Breaker plugin during the installation phase of the HttpClient. Both global and named configurations can be set.

Global, if registered, will be the default choice, whereas registering with a specific name allows you to apply it to specific services

```kotlin
HttpClient {
   install(CircuitBreaking) {
      config.global {
         failureThreshold = 10
         halfOpenFailureThreshold = 5
         resetInterval = 100.milliseconds
      }

      config.register("strict".toCircuitBreakerName()) {
         failureThreshold = 2
         halfOpenFailureThreshold = 1
         resetInterval = 1.seconds
      }
   }
}
```

You can then make requests using the circuit breaker, either by calling `HttpClient.requestWithCircuitBreaker` 

```kotlin
client.requestWithCircuitBreaker {
   method = Get
   url("https://service.com")
}

client.requestWithCircuitBreaker(name = "strict".toCircuitBreakerName()) {
    method = Get
    url("https://unreliable-service.com")
}
```

Or `HttpRequestBuilder.withCircuitBreaker` 
```kotlin
client.get("https://service.com") {
   withCircuitBreaker()
}

client.get("https://unreliable-service.com") {
   withCircuitBreaker(name = "strict".toCircuitBreakerName())
}
```

### Configuration Options:

The following are the configurable parameters:

- `failureThreshold`: The number of failures allowed before moving the circuit to the `HALF_OPEN` state for the global configuration.

- `halfOpenFailureThreshold`: The number of failures allowed in the `HALF_OPEN` state for the global configuration.

- `resetInterval`: The time to wait before transitioning from `OPEN` to `HALF_OPEN` state for the global configuration.

- `failureTrigger`: What is considered a failure. default is [HttpResponse.status] >= 300

## Important Notes:

- Ensure you have distinct names for each circuit breaker to prevent conflicts.

- Tweak the configuration based on the reliability and response times of the services you're interfacing with.

- Continuously monitor and adjust configurations for optimum resilience and performance.
