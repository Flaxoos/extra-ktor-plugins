# Circuit Breaker Plugin for Ktor Client

<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/ktor-client-circuit-breaker/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/93.58-green?logo=kotlin&label=koverage&style=flat)</a>

---

The Circuit Breaker pattern is a crucial part of modern software architecture, allowing you to gracefully handle
potential failures when communicating with external services. The Ktor Circuit Breaker plugin is an implementation of
this pattern, providing you with an easy way to incorporate this mechanism into your Ktor clients.

---

## Features:

- **Flexible Configuration**: Define both global and specific circuit breaker configurations.

- **Support for Named Circuit Breakers**: Allows for specific configurations for different endpoints or services.

- **Tight Integration with Ktor Client**: Uses familiar Ktor constructs.

- **Backed by Arrow**: each circuit breaker is an [Arrow `CircuitBreaker`](https://arrow-kt.io/learn/resilience/circuitbreaker/).

- **Failed requests count**: a request that throws, such as a timeout or a refused connection, counts as a failure, as
  does a response `failureTrigger` matches.

- **One test call**: once `resetInterval` has passed, an open circuit lets one call through and rejects the rest until
  it is answered. Its success closes the circuit, and its failure opens it again.

## How to Use:

You can configure the Circuit Breaker plugin during the installation phase of the HttpClient. Both global and named
configurations can be set.

Global, if registered, will be the default choice, whereas registering with a specific name allows you to apply it to
specific services

```kotlin
HttpClient {
   install(CircuitBreaking) {
      config.global {
         failureThreshold = 10
         resetInterval = 100.milliseconds
      }

      config.register("strict".toCircuitBreakerName()) {
         failureThreshold = 2
         resetInterval = 1.seconds
         backoffFactor = 2.0
         maxResetInterval = 1.minutes
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

- `failureThreshold`: How many failures in a row are tolerated; the next one opens the circuit. A success starts the
  count again.

- `resetInterval`: How long an open circuit rejects calls before it lets one test call through.

- `backoffFactor`: What each failed test call multiplies `resetInterval` by. The default, 1.0, keeps it fixed.

- `maxResetInterval`: The longest `resetInterval` grows to.

- `timeSource`: Where the circuit breaker reads the time from, the monotonic clock by default; a test can pass its own.

- `failureTrigger`: What is considered a failure. default is [HttpResponse.status] >= 300

- `onStateChange`: Called with the circuit breaker's name and new state (`CLOSED`, `OPEN` or `HALF_OPEN`) on each
  change, for logging or metrics. It runs inside the request that changed the state, so keep it quick; a failure in it
  is logged, not passed to the request.

- `halfOpenFailureThreshold`: Deprecated and not used, since an open circuit lets one test call through.

A rejected request throws `CircuitBreakerException`, which names the circuit breaker and states, as `retryAfter`, how
long until it lets a test call through.

## Important Notes:

- Ensure you have distinct names for each circuit breaker to prevent conflicts.

- Tweak the configuration based on the reliability and response times of the services you're interfacing with.

- Continuously monitor and adjust configurations for optimum resilience and performance.
