package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.inmemory

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingPluginTest

class InMemoryLockManagerTest : TaskSchedulingPluginTest() {
    override suspend fun clean() {}

    init {
        context("in-memory lock manager") {
            testTaskScheduling {
                inMemory()
            }
        }
    }
}
