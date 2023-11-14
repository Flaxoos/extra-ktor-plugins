---
title: ConcurrentFixedSizeWeightedQueue
---
//[extra-ktor-plugins](../../../index.md)/[io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations](../index.md)/[ConcurrentFixedSizeWeightedQueue](index.md)



# ConcurrentFixedSizeWeightedQueue



[common]\
class [ConcurrentFixedSizeWeightedQueue](index.md)&lt;[T](index.md)&gt;(maxWeight: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md))

A fixed size queue of weighted entries with safe access



## Constructors


| | |
|---|---|
| [ConcurrentFixedSizeWeightedQueue](-concurrent-fixed-size-weighted-queue.md) | [common]<br>constructor(maxWeight: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)) |


## Functions


| Name | Summary |
|---|---|
| [peekNext](peek-next.md) | [common]<br>fun [peekNext](peek-next.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.md)&lt;[T](index.md), [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)&gt;? |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.md) |
| [trimWhere](trim-where.md) | [common]<br>fun [trimWhere](trim-where.md)(comparison: ([T](index.md)) -&gt; [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.md)): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.md)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.md)&lt;[T](index.md), [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md)&gt;&gt;? |
| [tryAdd](try-add.md) | [common]<br>fun [tryAdd](try-add.md)(t: [T](index.md), weight: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/index.md) = 1.0): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.md) |

