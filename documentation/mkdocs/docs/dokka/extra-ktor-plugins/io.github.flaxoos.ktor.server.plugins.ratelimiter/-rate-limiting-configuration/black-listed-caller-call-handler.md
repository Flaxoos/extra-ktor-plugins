---
title: blackListedCallerCallHandler
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[RateLimitingConfiguration](index.md)/[blackListedCallerCallHandler](black-listed-caller-call-handler.md)



# blackListedCallerCallHandler



[common]\
var [blackListedCallerCallHandler](black-listed-caller-call-handler.md): suspend [RateLimitingConfiguration](index.md).(ApplicationCall) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.md)



The call handler for blacklisted Callers, use to define the response for blacklisted Callers, default is respond with 403




