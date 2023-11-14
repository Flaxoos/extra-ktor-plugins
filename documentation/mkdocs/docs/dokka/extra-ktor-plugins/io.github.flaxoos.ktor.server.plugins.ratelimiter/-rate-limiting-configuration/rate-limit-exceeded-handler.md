---
title: rateLimitExceededHandler
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimitingConfiguration](index.md)/[rateLimitExceededHandler](rate-limit-exceeded-handler.md)



# rateLimitExceededHandler



[common]\
val [rateLimitExceededHandler](rate-limit-exceeded-handler.md): suspend ApplicationCall.([RateLimiterResponse.LimitedBy](../-rate-limiter-response/-limited-by/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)



The call handler for rate limited IPs, use to define the response for rate limited IPs. The default is to respond with 429 and appropriate X-RateLimit headers




