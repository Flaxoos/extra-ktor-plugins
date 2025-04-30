---
title: SlidingWindow
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[SlidingWindow](index.md)/[SlidingWindow](-sliding-window.md)

# SlidingWindow

[common]\
constructor(rate: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md),
capacity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md),
callVolumeUnit: [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md),
clock: () -&gt; [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md) = { now()
.toEpochMilliseconds() })




