---
title: SlidingWindow
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[SlidingWindow](index.md)



# SlidingWindow



[common]\
data class [SlidingWindow](index.md)(val rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md), val clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() }) : [RateLimiter](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/index.md)

Sliding window, allows a given weight of calls to be made over a given duration.



The configured [rate](rate.md) will be the time window over which the calls will be counted. The call weight is defined by the [callVolumeUnit](call-volume-unit.md)



## Constructors


| | |
|---|---|
| [SlidingWindow](-sliding-window.md) | [common]<br>constructor(rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md), clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() }) |


## Properties


| Name | Summary |
|---|---|
| [callVolumeUnit](call-volume-unit.md) | [common]<br>open override val [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md)<br>In what unit are calls measured |
| [capacity](capacity.md) | [common]<br>open override val [capacity](capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum capacity, as measured in [CallVolumeUnit.Calls](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-calls/index.md) or [CallVolumeUnit.Bytes](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-bytes/index.md) |
| [clock](clock.md) | [common]<br>open override val [clock](clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider |
| [rate](rate.md) | [common]<br>open override val [rate](rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>Desired change over time |


## Functions


| Name | Summary |
|---|---|
| [id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md) | [common]<br>fun ApplicationCall.[id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |
| [tryAccept](try-accept.md) | [common]<br>open suspend override fun [tryAccept](try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter-response/index.md)<br>Try to accept a call |

