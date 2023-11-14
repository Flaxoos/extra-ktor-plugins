---
title: RateLimiterConfiguration
---
//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[RateLimitingConfiguration](../index.md)/[RateLimiterConfiguration](index.md)



# RateLimiterConfiguration



[common]\
class [RateLimiterConfiguration](index.md)(var type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [RateLimiter](../../-rate-limiter/index.md)&gt; = TokenBucket::class, var rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) = INFINITE, var capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = Int.MAX_VALUE, var clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() }, var callVolumeUnit: [CallVolumeUnit](../../-call-volume-unit/index.md) = CallVolumeUnit.Calls())



## Constructors


| | |
|---|---|
| [RateLimiterConfiguration](-rate-limiter-configuration.md) | [common]<br>constructor(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [RateLimiter](../../-rate-limiter/index.md)&gt; = TokenBucket::class, rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) = INFINITE, capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = Int.MAX_VALUE, clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() }, callVolumeUnit: [CallVolumeUnit](../../-call-volume-unit/index.md) = CallVolumeUnit.Calls()) |


## Properties


| Name | Summary |
|---|---|
| [callVolumeUnit](call-volume-unit.md) | [common]<br>var [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../../-call-volume-unit/index.md)<br>The unit by which the rate limiter capacity is measured, not applicable for [LeakyBucket](../../../io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations/-leaky-bucket/index.md) |
| [capacity](capacity.md) | [common]<br>var [capacity](capacity.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>The rate limiter capacity |
| [clock](clock.md) | [common]<br>var [clock](clock.md): () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)<br>A time provider in milliseconds |
| [rate](rate.md) | [common]<br>var [rate](rate.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>The rate limiter rate |
| [type](type.md) | [common]<br>var [type](type.md): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)&lt;out [RateLimiter](../../-rate-limiter/index.md)&gt;<br>The rate limiter implementation |

