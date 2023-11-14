---
title: tryAccept
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[TokenBucket](index.md)/[tryAccept](try-accept.md)



# tryAccept



[common]\
open suspend override fun [tryAccept](try-accept.md)(call: ApplicationCall): [RateLimiterResponse](../../io.github.flaxoos.ktor.server.plugins.ratelimiter/-rate-limiter-response/index.md)



Try to accept a call



#### Return



response detailing if the call was accepted and details in the case of rejection



#### Parameters


common

| | |
|---|---|
| call | the call to accept |




