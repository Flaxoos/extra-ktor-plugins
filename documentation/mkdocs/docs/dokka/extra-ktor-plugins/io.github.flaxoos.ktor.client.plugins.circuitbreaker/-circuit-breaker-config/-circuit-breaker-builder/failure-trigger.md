---
title: failureTrigger
---
//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.client.plugins.circuitbreaker](../../index.md)/[CircuitBreakerConfig](../index.md)/[CircuitBreakerBuilder](index.md)/[failureTrigger](failure-trigger.md)



# failureTrigger



[common]\
var [failureTrigger](failure-trigger.md): HttpResponse.() -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)



What is considered a failure. default is HttpResponse.status>= 300




