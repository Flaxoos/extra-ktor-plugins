package io.github.flaxoos.ktor.server.plugins.taskscheduler.integration

import kotlinx.coroutines.sync.Mutex


fun interface LockRegistry {
    /**
     * Obtains the lock associated with the parameter object.
     * @param lockKey The object with which the lock is associated.
     * @return The associated lock.
     */
    fun obtain(lockKey: Any): Mutex?
}

