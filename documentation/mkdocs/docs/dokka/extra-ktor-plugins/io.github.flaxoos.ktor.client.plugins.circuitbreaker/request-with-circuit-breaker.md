---
title: requestWithCircuitBreaker
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.client.plugins.circuitbreaker](index.md)/[requestWithCircuitBreaker](request-with-circuit-breaker.md)

# requestWithCircuitBreaker

[common]\
suspend fun HttpClient.[requestWithCircuitBreaker](request-with-circuit-breaker.md)(
name: [CircuitBreakerName](-circuit-breaker-name/index.md) = CIRCUIT_BREAKER_NAME_GLOBAL, block: HttpRequestBuilder.()
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)): HttpResponse

Make a request with the CircuitBreaker with the given [CircuitBreakerName](-circuit-breaker-name/index.md)




