---
title: TokenBucket
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[TokenBucket](index.md)

# TokenBucket

[common]\
class [TokenBucket](index.md)(log: KLogger = logger, val
rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val
callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md) =
CallVolumeUnit.Calls(), val capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val
clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now()
.toEpochMilliseconds() }) : [Bucket](../-bucket/index.md)

Token Bucket, allows varying rate of delivery, Better for handling bursts, but can be exploited by malicious clients to
consume all the capacity at once.

The configured Bucket.rate will be the rate at which tokens will be added to the bucket, until full.

CallVolumeUnit can
be [CallVolumeUnit.Calls](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-calls/index.md)
with optional call weight factor,
or [CallVolumeUnit.Bytes](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-bytes/index.md),
specifying how many bytes to add at every refill

## Constructors

|                                 |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [TokenBucket](-token-bucket.md) | [common]<br>constructor(log: KLogger = logger, rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md) = CallVolumeUnit.Calls(), capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now().toEpochMilliseconds() }) |

## Properties

| Name                                  | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callVolumeUnit](call-volume-unit.md) | [common]<br>open override val [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md)<br>The token to be added at every refill, can be [CallVolumeUnit.Calls](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-calls/index.md) with optional call weight factor, or [CallVolumeUnit.Bytes](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-bytes/index.md), specifying how many bytes to add at every refill |
| [capacity](../-bucket/capacity.md)    | [common]<br>override val [capacity](../-bucket/capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum capacity, as measured in CallVolumeUnit.Calls or CallVolumeUnit.Bytes                                                                                                                                                                                                                                                                                                                            |
| [clock](../-bucket/clock.md)          | [common]<br>override val [clock](../-bucket/clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider in milliseconds                                                                                                                                                                                                                                                                                                                                                                         |
| [rate](rate.md)                       | [common]<br>open override val [rate](rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>At what rate should token be added to the bucket                                                                                                                                                                                                                                                                                                                                                            |

## Functions

| Name                                                                              | Summary                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md) | [common]<br>fun ApplicationCall.[id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                             |
| [tryAccept](try-accept.md)                                                        | [common]<br>open suspend override fun [tryAccept](try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter-response/index.md)<br>Try to accept a call |

