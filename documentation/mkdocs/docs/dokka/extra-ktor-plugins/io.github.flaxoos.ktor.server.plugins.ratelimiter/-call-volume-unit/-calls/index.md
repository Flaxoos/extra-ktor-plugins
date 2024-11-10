---
title: Calls
---

//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[CallVolumeUnit](../index.md)/[Calls](index.md)

# Calls

[common]\
open class [Calls](index.md)(val callWeighting: ApplicationCall.()
-&gt; [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md) = {
1.0 }) : [CallVolumeUnit](../index.md)

Volume is measured in number of calls, with an optional call weighting function to give more weight to a call based on
any of it's properties

## Constructors

|                    |                                                                                                                                                           |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Calls](-calls.md) | [common]<br>constructor(callWeighting: ApplicationCall.() -&gt; [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md) = { 1.0 }) |

## Properties

| Name                               | Summary                                                                                                                                                     |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callWeighting](call-weighting.md) | [common]<br>val [callWeighting](call-weighting.md): ApplicationCall.() -&gt; [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md) |
| [name](name.md)                    | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md)                               |
| [size](size.md)                    | [common]<br>open override val [size](size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) = 1                                 |

## Functions

| Name                     | Summary                                                                                                                                                                                                                  |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callSize](call-size.md) | [common]<br>open suspend override fun [callSize](call-size.md)(call: ApplicationCall): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size of the given call as measured by this unit |
| [size](../size.md)       | [common]<br>open fun [size](../size.md)(): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size as Double                                                                              |

