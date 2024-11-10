---
title: CallVolumeUnit
---

//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../index.md)/[CallVolumeUnit](index.md)

# CallVolumeUnit

interface [CallVolumeUnit](index.md)

#### Inheritors

|                          |
|--------------------------|
| [Calls](-calls/index.md) |
| [Bytes](-bytes/index.md) |

## Types

| Name                     | Summary                                                                                                                                                                                                                                                                                                                                                             |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Bytes](-bytes/index.md) | [common]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)<br>value class [Bytes](-bytes/index.md)(val size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)) : [CallVolumeUnit](index.md)<br>Volume is measured in number of bytes of request                                                 |
| [Calls](-calls/index.md) | [common]<br>open class [Calls](-calls/index.md)(val callWeighting: ApplicationCall.() -&gt; [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md) = { 1.0 }) : [CallVolumeUnit](index.md)<br>Volume is measured in number of calls, with an optional call weighting function to give more weight to a call based on any of it's properties |

## Properties

| Name            | Summary                                                                                                                  |
|-----------------|--------------------------------------------------------------------------------------------------------------------------|
| [name](name.md) | [common]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |
| [size](size.md) | [common]<br>abstract val [size](size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)       |

## Functions

| Name                     | Summary                                                                                                                                                                                                             |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [callSize](call-size.md) | [common]<br>abstract suspend fun [callSize](call-size.md)(call: ApplicationCall): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size of the given call as measured by this unit |
| [size](size.md)          | [common]<br>open fun [size](size.md)(): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size as Double                                                                            |

