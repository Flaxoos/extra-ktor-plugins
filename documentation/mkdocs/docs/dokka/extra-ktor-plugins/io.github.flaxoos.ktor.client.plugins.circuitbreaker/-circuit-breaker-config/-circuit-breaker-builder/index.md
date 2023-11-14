---
title: CircuitBreakerBuilder
---
//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.client.plugins.circuitbreaker](../../index.md)/[CircuitBreakerConfig](../index.md)/[CircuitBreakerBuilder](index.md)



# CircuitBreakerBuilder



[common]\
class [CircuitBreakerBuilder](index.md)



## Constructors


| | |
|---|---|
| [CircuitBreakerBuilder](-circuit-breaker-builder.md) | [common]<br>constructor() |


## Properties


| Name | Summary |
|---|---|
| [failureThreshold](failure-threshold.md) | [common]<br>var [failureThreshold](failure-threshold.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>How many failures are to be tolerated before the circuit moves to HALF_OPEN. |
| [failureTrigger](failure-trigger.md) | [common]<br>var [failureTrigger](failure-trigger.md): HttpResponse.() -&gt; [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md)<br>What is considered a failure. default is HttpResponse.status>= 300 |
| [halfOpenFailureThreshold](half-open-failure-threshold.md) | [common]<br>var [halfOpenFailureThreshold](half-open-failure-threshold.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)<br>How many attempts are allowed in HALF_OPEN state. |
| [resetInterval](reset-interval.md) | [common]<br>var [resetInterval](reset-interval.md): [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/index.md)<br>How long to wait before moving from OPEN to HALF_OPEN. |

