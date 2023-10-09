package com.example

import net.javacrumbs.shedlock.support.annotation.Nullable
import java.time.Duration
import java.util.Deque
import java.util.LinkedList

object LockExtender {
    // Using deque here instead of a simple thread local to be able to handle nested locks.
    private val activeLocks = ThreadLocal.withInitial<Deque<SimpleLock>> { LinkedList() }

    /**
     * Extends active lock. Is based on a thread local variable, so it might not work in case of async processing.
     * In case of nested locks, extends the innermost lock.
     *
     * @throws LockCanNotBeExtendedException when the lock can not be extended due to expired lock
     * @throws NoActiveLockException         when there is no active lock in the thread local
     * @throws UnsupportedOperationException when the LockProvider does not support lock extension.
     */
    fun extendActiveLock(lockAtMostFor: Duration?, lockAtLeastFor: Duration?) {
        val lock = locks().peekLast() ?: throw NoActiveLockException()
        val newLock = lock.extend(lockAtMostFor, lockAtLeastFor)
        if (newLock!!.isPresent) {
            // removing and adding here should be safe as it's a thread local variable and the changes are only visible in the current thread.
            locks().removeLast()
            locks().addLast(newLock.get())
        } else {
            throw LockCanNotBeExtendedException()
        }
    }

    private fun locks(): Deque<SimpleLock> {
        return activeLocks.get()
    }

    fun startLock(lock: SimpleLock) {
        locks().addLast(lock)
    }

    @Nullable
    fun endLock(): SimpleLock {
        val lock = locks().pollLast()
        // we want to clean up the thread local variable when there are no locks
        if (locks().isEmpty()) {
            activeLocks.remove()
        }
        return lock
    }

    open class LockExtensionException(message: String?) : RuntimeException(message)
    class NoActiveLockException :
        LockExtensionException("No active lock in current thread, please make sure that you execute LockExtender.extendActiveLock in locked context.")

    class LockCanNotBeExtendedException :
        LockExtensionException("Lock can not be extended, most likely it already expired.")
}
