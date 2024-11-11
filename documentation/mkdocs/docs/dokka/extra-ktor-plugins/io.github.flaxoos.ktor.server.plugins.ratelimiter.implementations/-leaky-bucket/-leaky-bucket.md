---
title: LeakyBucket
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[LeakyBucket](index.md)/[LeakyBucket](-leaky-bucket.md)

# LeakyBucket

[common]\
constructor(log: KLogger = logger,
rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md),
capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md), clock: ()
-&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { Clock.System.now()
.toEpochMilliseconds() })




