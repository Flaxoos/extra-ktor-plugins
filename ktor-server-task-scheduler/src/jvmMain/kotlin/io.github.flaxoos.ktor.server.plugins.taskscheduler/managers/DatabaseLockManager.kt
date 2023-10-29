package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers

import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.TaskLockKey
import korlibs.time.DateTime

public abstract class DatabaseLockManager<T : DatabaseTaskLockKey> : LockManager<T>

public interface DatabaseTaskLockKey : TaskLockKey {
    public val name: String
    public val lockedAt: DateTime?
    public val lockUntil: DateTime?
}