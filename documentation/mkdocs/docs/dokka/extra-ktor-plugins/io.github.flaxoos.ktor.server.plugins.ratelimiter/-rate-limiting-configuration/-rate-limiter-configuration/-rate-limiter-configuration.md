---
title: RateLimiterConfiguration
---

//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[RateLimitingConfiguration](../index.md)/[RateLimiterConfiguration](index.md)/[RateLimiterConfiguration](-rate-limiter-configuration.md)

# RateLimiterConfiguration

[common]\
constructor(type: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.md)
&lt;out [RateLimiter](../../-rate-limiter/index.md)&gt; = TokenBucket::class,
rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md) = INFINITE,
capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = Int.MAX_VALUE, clock: ()
-&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now().toEpochMilliseconds() },
callVolumeUnit: [CallVolumeUnit](../../-call-volume-unit/index.md) = CallVolumeUnit.Calls())




