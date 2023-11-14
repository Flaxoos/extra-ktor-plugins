---
title: RateLimiterResponse
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimiterResponse](index.md)



# RateLimiterResponse

interface [RateLimiterResponse](index.md)

#### Inheritors


| |
|---|
| [NotLimited](-not-limited/index.md) |
| [LimitedBy](-limited-by/index.md) |


## Types


| Name | Summary |
|---|---|
| [LimitedBy](-limited-by/index.md) | [common]<br>data class [LimitedBy](-limited-by/index.md)(val rateLimiter: [RateLimiter](../-rate-limiter/index.md), val exceededBy: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md), val resetIn: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)) : [RateLimiterResponse](index.md) |
| [NotLimited](-not-limited/index.md) | [common]<br>data class [NotLimited](-not-limited/index.md)(val rateLimiter: [RateLimiter](../-rate-limiter/index.md), val remaining: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.md)? = null) : [RateLimiterResponse](index.md) |


## Properties


| Name | Summary |
|---|---|
| [rateLimiter](rate-limiter.md) | [common]<br>abstract val [rateLimiter](rate-limiter.md): [RateLimiter](../-rate-limiter/index.md) |

