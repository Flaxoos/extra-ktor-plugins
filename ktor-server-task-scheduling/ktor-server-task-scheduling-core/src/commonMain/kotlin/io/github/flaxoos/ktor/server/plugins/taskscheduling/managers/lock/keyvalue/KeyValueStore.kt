package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue

/**
 * Abstraction for a key-value store with SET-if-not-exists semantics and TTL support.
 *
 * This interface provides a common abstraction for different key-value storage mechanisms
 * (e.g., Redis, in-memory maps) that support atomic set-if-not-exists operations with
 * time-to-live functionality.
 */
public interface KeyValueStore {
    /**
     * Attempt to set a key-value pair only if the key doesn't already exist,
     * with a time-to-live in milliseconds.
     *
     * This operation should be atomic - either the key is set successfully (and returns true),
     * or it already exists (and returns false). The TTL is calculated from the system clock
     * at the time of the call.
     *
     * @param key The key to set
     * @param value The value to set
     * @param ttlMs Time-to-live in milliseconds from now
     * @return true if the key was set successfully, false if it already exists
     */
    public suspend fun setIfNotExist(
        key: String,
        value: String,
        ttlMs: Long,
    ): Boolean
}
