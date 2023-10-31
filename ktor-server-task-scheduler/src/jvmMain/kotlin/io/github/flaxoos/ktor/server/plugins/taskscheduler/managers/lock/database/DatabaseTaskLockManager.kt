package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.TaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import korlibs.time.DateTime

public abstract class DatabaseTaskLockManager<KEY : DatabaseTaskLockKey> : TaskLockManager<KEY>()

public interface DatabaseTaskLockKey : TaskLockKey {
    public val name: String
    public val concurrencyIndex: Int
    public val lockedAt: DateTime
}

@TaskSchedulerDsl
public abstract class DatabaseTaskLockManagerConfiguration : TaskLockManagerConfiguration()
