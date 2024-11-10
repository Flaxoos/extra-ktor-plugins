---
title: io.github.flaxoos.ktor.server.plugins.taskscheduling
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling](index.md)

# Package-level declarations

## Types

| Name                                                                   | Summary                                                                                                                                                                                                                                        |
|------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [TaskConfiguration](-task-configuration/index.md)                      | [common]<br>class [TaskConfiguration](-task-configuration/index.md)<br>Configuration for a [Task](../io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks/-task/index.md)                                                                |
| [TaskFreqMs](-task-freq-ms/index.md)                                   | [jvm]<br>@[JvmInline](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-inline/index.md)<br>value class [TaskFreqMs](-task-freq-ms/index.md)(val value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.md)) |
| [TaskSchedulingConfiguration](-task-scheduling-configuration/index.md) | [common]<br>open class [TaskSchedulingConfiguration](-task-scheduling-configuration/index.md)<br>Configuration for [TaskScheduling](-task-scheduling.md)                                                                                       |
| [TaskSchedulingDsl](-task-scheduling-dsl/index.md)                     | [common]<br>@[DslMarker](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/index.md)<br>annotation class [TaskSchedulingDsl](-task-scheduling-dsl/index.md)                                                                      |
| [TaskSchedulingPluginTest](-task-scheduling-plugin-test/index.md)      | [jvm]<br>abstract class [TaskSchedulingPluginTest](-task-scheduling-plugin-test/index.md) : FunSpec                                                                                                                                            |

## Properties

| Name                                  | Summary                                                                                                                                                                         |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [logger](logger.md)                   | [jvm]<br>val [logger](logger.md): KLogger                                                                                                                                       |
| [TaskScheduling](-task-scheduling.md) | [common]<br>val [TaskScheduling](-task-scheduling.md): ApplicationPlugin&lt;[TaskSchedulingConfiguration](-task-scheduling-configuration/index.md)&gt;<br>Task scheduler plugin |

