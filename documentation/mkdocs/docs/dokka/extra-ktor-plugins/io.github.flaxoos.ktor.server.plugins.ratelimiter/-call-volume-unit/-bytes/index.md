---
title: Bytes
---
//[extra-ktor-plugins](../../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter](../../index.md)/[CallVolumeUnit](../index.md)/[Bytes](index.md)



# Bytes



[common]\
@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)



value class [Bytes](index.md)(val size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)) : [CallVolumeUnit](../index.md)

Volume is measured in number of bytes of request



## Constructors


| | |
|---|---|
| [Bytes](-bytes.md) | [common]<br>constructor(size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)) |


## Properties


| Name | Summary |
|---|---|
| [name](name.md) | [common]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |
| [size](size.md) | [common]<br>open override val [size](size.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md) |


## Functions


| Name | Summary |
|---|---|
| [callSize](call-size.md) | [common]<br>open suspend override fun [callSize](call-size.md)(call: ApplicationCall): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size of the given call as measured by this unit |
| [size](../size.md) | [common]<br>open fun [size](../size.md)(): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)<br>Size as Double |

