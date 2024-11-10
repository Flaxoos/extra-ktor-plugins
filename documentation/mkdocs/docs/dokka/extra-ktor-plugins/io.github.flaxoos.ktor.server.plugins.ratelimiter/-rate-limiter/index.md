---
title: RateLimiter
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimiter](index.md)

# RateLimiter

abstract class [RateLimiter](index.md)

#### Inheritors

|                                                                                                                   |
|-------------------------------------------------------------------------------------------------------------------|
| [Bucket](../../io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations/-bucket/index.md)                |
| [SlidingWindow](../../io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations/-sliding-window/index.md) |

## Constructors

|                                 |                           |
|---------------------------------|---------------------------|
| [RateLimiter](-rate-limiter.md) | [common]<br>constructor() |

## Properties

| Name                                  | Summary                                                                                                                                                                                                                                                                                         |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callVolumeUnit](call-volume-unit.md) | [common]<br>abstract val [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../-call-volume-unit/index.md)<br>In what unit are calls measured                                                                                                                                              |
| [capacity](capacity.md)               | [common]<br>abstract val [capacity](capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum capacity, as measured in [CallVolumeUnit.Calls](../-call-volume-unit/-calls/index.md) or [CallVolumeUnit.Bytes](../-call-volume-unit/-bytes/index.md) |
| [clock](clock.md)                     | [common]<br>open val [clock](clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider in milliseconds                                                                                                                                  |
| [rate](rate.md)                       | [common]<br>abstract val [rate](rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>Desired change over time                                                                                                                                   |

## Functions

| Name                       | Summary                                                                                                                                                               |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [id](id.md)                | [common]<br>fun ApplicationCall.[id](id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                                         |
| [tryAccept](try-accept.md) | [common]<br>abstract suspend fun [tryAccept](try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../-rate-limiter-response/index.md)<br>Try to accept a call |

