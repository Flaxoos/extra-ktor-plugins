---
title: io.github.flaxoos.ktor.server.plugins.taskscheduling.managers
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling.managers](index.md)

# Package-level declarations

## Types

| Name                                                             | Summary                                                                                                                                                                                                                    |
|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [TaskExecutionToken](-task-execution-token/index.md)             | [common]<br>interface [TaskExecutionToken](-task-execution-token/index.md)<br>Represents a token that can be used to grant permission to execute a task                                                                    |
| [TaskManager](-task-manager/index.md)                            | [common]<br>abstract class [TaskManager](-task-manager/index.md)&lt;[TASK_EXECUTION_TOKEN](-task-manager/index.md) : [TaskExecutionToken](-task-execution-token/index.md)&gt; : Closeable                                  |
| [TaskManagerConfiguration](-task-manager-configuration/index.md) | [common]<br>abstract class [TaskManagerConfiguration](-task-manager-configuration/index.md)&lt;[TASK_EXECUTION_TOKEN](-task-manager-configuration/index.md)&gt;<br>Configuration for [TaskManager](-task-manager/index.md) |

