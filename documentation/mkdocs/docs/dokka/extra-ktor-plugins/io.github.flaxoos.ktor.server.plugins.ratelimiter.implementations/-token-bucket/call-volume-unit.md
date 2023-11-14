---
title: callVolumeUnit
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[TokenBucket](index.md)/[callVolumeUnit](call-volume-unit.md)



# callVolumeUnit



[common]\
open override val [callVolumeUnit](call-volume-unit.md): [CallVolumeUnit](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/index.md)



The token to be added at every refill, can be [CallVolumeUnit.Calls](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-calls/index.md) with optional call weight factor, or [CallVolumeUnit.Bytes](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-call-volume-unit/-bytes/index.md), specifying how many bytes to add at every refill




