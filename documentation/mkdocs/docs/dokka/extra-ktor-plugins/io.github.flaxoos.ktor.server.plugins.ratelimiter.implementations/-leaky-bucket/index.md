---
title: LeakyBucket
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[LeakyBucket](index.md)

# LeakyBucket

[common]\
class [LeakyBucket](index.md)(log: KLogger = logger, val
rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val
capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val clock: ()
-&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now()
.toEpochMilliseconds() }) : [Bucket](../-bucket/index.md)

Leaky Bucket, allows for constant rate of delivery, Fair distribution between clients, but not ideal for handling bursts

The configured [rate](rate.md) will be the rate in which requests will leak from the bucket, when not empty. This means
the call will be suspended until it leaks out of the bucket, at which point the [tryAccept](try-accept.md) function
would return. No timeout is set, so the call can be suspended indefinitely in theory, and it is up to the server or
client to implement a timeout.

CallVolumeUnit is always Calls with weighting of 1.0.

## Constructors

|                                 |                                                                                                                                                                                                                                                                                                                                                                              |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [LeakyBucket](-leaky-bucket.md) | [common]<br>constructor(log: KLogger = logger, rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now().toEpochMilliseconds() }) |

## Properties

| Name                                  | Summary                                                                                                                                                                                                                    |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callVolumeUnit](call-volume-unit.md) | [common]<br>open override val [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md)<br>In what unit are calls measured               |
| [capacity](../-bucket/capacity.md)    | [common]<br>override val [capacity](../-bucket/capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum capacity, as measured in CallVolumeUnit.Calls or CallVolumeUnit.Bytes |
| [clock](../-bucket/clock.md)          | [common]<br>override val [clock](../-bucket/clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider in milliseconds                                              |
| [rate](rate.md)                       | [common]<br>open override val [rate](rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>Desired change over time                                                         |

## Functions

| Name                                                                              | Summary                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md) | [common]<br>fun ApplicationCall.[id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                             |
| [tryAccept](try-accept.md)                                                        | [common]<br>open suspend override fun [tryAccept](try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter-response/index.md)<br>Try to accept a call |

