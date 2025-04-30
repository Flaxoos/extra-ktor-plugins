---
title: io.github.flaxoos.ktor.client.plugins.circuitbreaker
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.client.plugins.circuitbreaker](index.md)

# Package-level declarations

## Types

| Name                                                           | Summary                                                                                                                                                                                                                                                                                                        |
|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CircuitBreakerConfig](-circuit-breaker-config/index.md)       | [common]<br>class [CircuitBreakerConfig](-circuit-breaker-config/index.md)<br>Configuration for CircuitBreaker.                                                                                                                                                                                                |
| [CircuitBreakerDsl](-circuit-breaker-dsl/index.md)             | [common]<br>@[DslMarker](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/index.md)<br>annotation class [CircuitBreakerDsl](-circuit-breaker-dsl/index.md)                                                                                                                                      |
| [CircuitBreakerException](-circuit-breaker-exception/index.md) | [common]<br>class [CircuitBreakerException](-circuit-breaker-exception/index.md)(failureThreshold: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)) : [Exception](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/index.md)                                            |
| [CircuitBreakerName](-circuit-breaker-name/index.md)           | [common]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)<br>value class [CircuitBreakerName](-circuit-breaker-name/index.md)(val value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md))<br>Value class for a CircuitBreaker name |

## Properties

| Name                                    | Summary                                                                                                                               |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| [CircuitBreaking](-circuit-breaking.md) | [common]<br>val [CircuitBreaking](-circuit-breaking.md): ClientPlugin&lt;[CircuitBreakerConfig](-circuit-breaker-config/index.md)&gt; |

## Functions

| Name                                                         | Summary                                                                                                                                                                                                                                                                                                                                                                                                                              |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [global](global.md)                                          | [common]<br>fun [CircuitBreakerConfig](-circuit-breaker-config/index.md).[global](global.md)(config: [CircuitBreakerConfig.CircuitBreakerBuilder](-circuit-breaker-config/-circuit-breaker-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Registers a global CircuitBreaker that is applied to the whole client                                                           |
| [register](register.md)                                      | [common]<br>fun [CircuitBreakerConfig](-circuit-breaker-config/index.md).[register](register.md)(name: [CircuitBreakerName](-circuit-breaker-name/index.md), config: [CircuitBreakerConfig.CircuitBreakerBuilder](-circuit-breaker-config/-circuit-breaker-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Register a CircuitBreaker with a given name                     |
| [requestWithCircuitBreaker](request-with-circuit-breaker.md) | [common]<br>suspend fun HttpClient.[requestWithCircuitBreaker](request-with-circuit-breaker.md)(name: [CircuitBreakerName](-circuit-breaker-name/index.md) = CIRCUIT_BREAKER_NAME_GLOBAL, block: HttpRequestBuilder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)): HttpResponse<br>Make a request with the CircuitBreaker with the given [CircuitBreakerName](-circuit-breaker-name/index.md) |
| [withCircuitBreaker](with-circuit-breaker.md)                | [common]<br>fun HttpRequestBuilder.[withCircuitBreaker](with-circuit-breaker.md)(name: [CircuitBreakerName](-circuit-breaker-name/index.md) = CIRCUIT_BREAKER_NAME_GLOBAL)<br>Apply the CircuitBreaker with the given [CircuitBreakerName](-circuit-breaker-name/index.md) to the request or the global one if name is given                                                                                                         |

