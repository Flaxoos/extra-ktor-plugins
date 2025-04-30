---
title: callAcceptedHandler
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimitingConfiguration](index.md)/[callAcceptedHandler](call-accepted-handler.md)

# callAcceptedHandler

[common]\
val [callAcceptedHandler](call-accepted-handler.md): suspend
ApplicationCall.([RateLimiterResponse.NotLimited](../-rate-limiter-response/-not-limited/index.md))
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)

The call handler for accepted calls, use to define the response for accepted calls, by default, adds appropriate
X-RateLimit headers




