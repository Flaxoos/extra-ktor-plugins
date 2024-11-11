---
title: io.github.flaxoos.ktor.server.plugins.ratelimiter
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](index.md)

# Package-level declarations

## Types

| Name                                                               | Summary                                                                                                                                                               |
|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CallVolumeUnit](-call-volume-unit/index.md)                       | [common]<br>interface [CallVolumeUnit](-call-volume-unit/index.md)                                                                                                    |
| [RateLimiter](-rate-limiter/index.md)                              | [common]<br>abstract class [RateLimiter](-rate-limiter/index.md)                                                                                                      |
| [RateLimiterResponse](-rate-limiter-response/index.md)             | [common]<br>interface [RateLimiterResponse](-rate-limiter-response/index.md)                                                                                          |
| [RateLimitingConfiguration](-rate-limiting-configuration/index.md) | [common]<br>class [RateLimitingConfiguration](-rate-limiting-configuration/index.md)<br>Rate limit plugin configuration.                                              |
| [RateLimitingDsl](-rate-limiting-dsl/index.md)                     | [common]<br>@[DslMarker](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/index.md)<br>annotation class [RateLimitingDsl](-rate-limiting-dsl/index.md) |

## Properties

| Name                              | Summary                                                                                                                                                                                                                                                                                                                              |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [RateLimiting](-rate-limiting.md) | [common]<br>val [RateLimiting](-rate-limiting.md): RouteScopedPlugin&lt;[RateLimitingConfiguration](-rate-limiting-configuration/index.md)&gt;<br>Rate limiting plugin, apply to route to provide route scoped rate limiting, see [RateLimitingConfiguration](-rate-limiting-configuration/index.md) for details on how to configure |

