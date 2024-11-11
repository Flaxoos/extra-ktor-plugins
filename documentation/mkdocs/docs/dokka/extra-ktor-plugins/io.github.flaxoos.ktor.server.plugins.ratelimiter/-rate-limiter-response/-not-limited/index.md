---
title: NotLimited
---

//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[RateLimiterResponse](../index.md)/[NotLimited](index.md)

# NotLimited

[common]\
data class [NotLimited](index.md)(val rateLimiter: [RateLimiter](../../-rate-limiter/index.md), val
remaining: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md)? =
null) : [RateLimiterResponse](../index.md)

## Constructors

|                               |                                                                                                                                                                                      |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [NotLimited](-not-limited.md) | [common]<br>constructor(rateLimiter: [RateLimiter](../../-rate-limiter/index.md), remaining: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md)? = null) |

## Properties

| Name                           | Summary                                                                                                                           |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| [rateLimiter](rate-limiter.md) | [common]<br>open override val [rateLimiter](rate-limiter.md): [RateLimiter](../../-rate-limiter/index.md)                         |
| [remaining](remaining.md)      | [common]<br>val [remaining](remaining.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md)? = null |

