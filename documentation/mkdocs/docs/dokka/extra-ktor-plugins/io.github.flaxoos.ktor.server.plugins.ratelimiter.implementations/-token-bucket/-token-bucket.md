---
title: TokenBucket
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[TokenBucket](index.md)/[TokenBucket](-token-bucket.md)



# TokenBucket



[common]\
constructor(log: KLogger = logger, rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md), callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md) = CallVolumeUnit.Calls(), capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now().toEpochMilliseconds() })




