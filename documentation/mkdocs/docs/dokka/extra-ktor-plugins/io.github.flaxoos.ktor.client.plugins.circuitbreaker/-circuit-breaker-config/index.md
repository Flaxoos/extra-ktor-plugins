---
title: CircuitBreakerConfig
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.client.plugins.circuitbreaker](../index.md)/[CircuitBreakerConfig](index.md)

# CircuitBreakerConfig

[common]\
class [CircuitBreakerConfig](index.md)

Configuration for CircuitBreaker.

## Constructors

|                                                    |                           |
|----------------------------------------------------|---------------------------|
| [CircuitBreakerConfig](-circuit-breaker-config.md) | [common]<br>constructor() |

## Types

| Name                                                       | Summary                                                                      |
|------------------------------------------------------------|------------------------------------------------------------------------------|
| [CircuitBreakerBuilder](-circuit-breaker-builder/index.md) | [common]<br>class [CircuitBreakerBuilder](-circuit-breaker-builder/index.md) |

## Functions

| Name                       | Summary                                                                                                                                                                                                                                                                                                                                                                |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [global](../global.md)     | [common]<br>fun [CircuitBreakerConfig](index.md).[global](../global.md)(config: [CircuitBreakerConfig.CircuitBreakerBuilder](-circuit-breaker-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Registers a global CircuitBreaker that is applied to the whole client                                          |
| [register](../register.md) | [common]<br>fun [CircuitBreakerConfig](index.md).[register](../register.md)(name: [CircuitBreakerName](../-circuit-breaker-name/index.md), config: [CircuitBreakerConfig.CircuitBreakerBuilder](-circuit-breaker-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md))<br>Register a CircuitBreaker with a given name |

