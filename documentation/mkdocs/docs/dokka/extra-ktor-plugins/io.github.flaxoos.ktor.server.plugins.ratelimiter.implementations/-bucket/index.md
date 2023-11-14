---
title: Bucket
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[Bucket](index.md)



# Bucket

sealed class [Bucket](index.md) : [RateLimiter](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/index.md)

#### Inheritors


| |
|---|
| [LeakyBucket](../-leaky-bucket/index.md) |
| [TokenBucket](../-token-bucket/index.md) |


## Properties


| Name | Summary |
|---|---|
| [callVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/call-volume-unit.md) | [common]<br>abstract val [callVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/call-volume-unit.md): [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md)<br>In what unit are calls measured |
| [capacity](capacity.md) | [common]<br>override val [capacity](capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The maximum capacity, as measured in CallVolumeUnit.Calls or CallVolumeUnit.Bytes |
| [clock](clock.md) | [common]<br>override val [clock](clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider in milliseconds |
| [rate](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/rate.md) | [common]<br>abstract val [rate](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>Desired change over time |


## Functions


| Name | Summary |
|---|---|
| [id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md) | [common]<br>fun ApplicationCall.[id](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |
| [tryAccept](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/try-accept.md) | [common]<br>abstract suspend fun [tryAccept](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter-response/index.md)<br>Try to accept a call |

