---
title: tryAccept
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimiter](index.md)/[tryAccept](try-accept.md)

# tryAccept

[common]\
abstract suspend fun [tryAccept](try-accept.md)(call:
ApplicationCall): [RateLimiterResponse](../-rate-limiter-response/index.md)

Try to accept a call

#### Return

response detailing if the call was accepted and details in the case of rejection

#### Parameters

common

|      |                    |
|------|--------------------|
| call | the call to accept |




