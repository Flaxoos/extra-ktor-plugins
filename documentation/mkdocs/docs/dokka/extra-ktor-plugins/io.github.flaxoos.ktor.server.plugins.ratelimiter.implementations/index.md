---
title: io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations
---
//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](index.md)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [Bucket](-bucket/index.md) | [common]<br>sealed class [Bucket](-bucket/index.md) : [RateLimiter](../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/index.md) |
| [ConcurrentFixedSizeWeightedQueue](-concurrent-fixed-size-weighted-queue/index.md) | [common]<br>class [ConcurrentFixedSizeWeightedQueue](-concurrent-fixed-size-weighted-queue/index.md)&lt;[T](-concurrent-fixed-size-weighted-queue/index.md)&gt;(maxWeight: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md))<br>A fixed size queue of weighted entries with safe access |
| [LeakyBucket](-leaky-bucket/index.md) | [common]<br>class [LeakyBucket](-leaky-bucket/index.md)(log: KLogger = logger, val rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now().toEpochMilliseconds() }) : [Bucket](-bucket/index.md)<br>Leaky Bucket, allows for constant rate of delivery, Fair distribution between clients, but not ideal for handling bursts |
| [SlidingWindow](-sliding-window/index.md) | [common]<br>data class [SlidingWindow](-sliding-window/index.md)(val rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val callVolumeUnit: [CallVolumeUnit](../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md), val clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() }) : [RateLimiter](../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter/index.md)<br>Sliding window, allows a given weight of calls to be made over a given duration. |
| [TokenBucket](-token-bucket/index.md) | [common]<br>class [TokenBucket](-token-bucket/index.md)(log: KLogger = logger, val rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), val callVolumeUnit: [CallVolumeUnit](../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md) = CallVolumeUnit.Calls(), val capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), val clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now().toEpochMilliseconds() }) : [Bucket](-bucket/index.md)<br>Token Bucket, allows varying rate of delivery, Better for handling bursts, but can be exploited by malicious clients to consume all the capacity at once. |

