---
title: LimitedBy
---

//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[RateLimiterResponse](../index.md)/[LimitedBy](index.md)

# LimitedBy

[common]\
data class [LimitedBy](index.md)(val rateLimiter: [RateLimiter](../../-rate-limiter/index.md), val
exceededBy: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md), val
resetIn: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val
message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)) : [RateLimiterResponse](../index.md)

## Constructors

|                             |                                                                                                                                                                                                                                                                                                                                                                          |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [LimitedBy](-limited-by.md) | [common]<br>constructor(rateLimiter: [RateLimiter](../../-rate-limiter/index.md), exceededBy: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md), resetIn: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)) |

## Properties

| Name                           | Summary                                                                                                                         |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| [exceededBy](exceeded-by.md)   | [common]<br>val [exceededBy](exceeded-by.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md)    |
| [message](message.md)          | [common]<br>val [message](message.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)           |
| [rateLimiter](rate-limiter.md) | [common]<br>open override val [rateLimiter](rate-limiter.md): [RateLimiter](../../-rate-limiter/index.md)                       |
| [resetIn](reset-in.md)         | [common]<br>val [resetIn](reset-in.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) |

