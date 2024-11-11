---
title: TaskScheduling
---

//[extra-ktor-plugins](../../index.md)/[io.github.flaxoos.ktor.server.plugins.taskscheduling](index.md)/[TaskScheduling](-task-scheduling.md)

# TaskScheduling

[common]\
val [TaskScheduling](-task-scheduling.md):
ApplicationPlugin&lt;[TaskSchedulingConfiguration](-task-scheduling-configuration/index.md)&gt;

Task scheduler plugin

Provides scheduling capabilities for a given set of scheduled tasks. The tasks are managed by some implementation
of [TaskManager](../io.github.flaxoos.ktor.server.plugins.taskscheduling.managers/-task-manager/index.md), that is
responsible for coordinating the execution of the tasks across the different instances of the application.




