package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.inmemory

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueStore
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueTaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueTaskLockManagerConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.util.collections.ConcurrentMap
import kotlinx.datetime.Clock
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * An in-memory implementation of [KeyValueTaskLockManager].
 *
 * This implementation is suitable for single-instance deployments where distributed locking
 * is not required. All locks are stored in memory and will be lost on application restart.
 *
 * **Important**: This manager provides locking within a single JVM/process across all
 * InMemoryLockManager instances. It does NOT provide distributed locking across separate
 * JVM processes or machines.
 *
 * @property name The name of the task manager
 * @property application The Ktor application instance
 * @property lockExpirationMs Lock expiration time in milliseconds. Locks older than this
 *           duration will be considered expired and can be re-acquired.
 * @property maxSize Maximum number of entries allowed in memory before forced cleanup.
 *           Default: 10,000 entries
 * @property cleanupProbability Probability (0-100) of triggering cleanup on each setIfNotExist call.
 *           Higher values = more frequent cleanup but slightly slower performance. Default: 10 (10%)
 */
public class InMemoryLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    lockExpirationMs: Long,
    maxSize: Int = 10_000,
    cleanupProbability: Int = 10,
) : KeyValueTaskLockManager(lockExpirationMs) {
    override val store: KeyValueStore = InMemoryKeyValueStore(maxSize, cleanupProbability)

    override fun close() {
        val kvStore = store as InMemoryKeyValueStore
        logger.debug { "Closing InMemoryLockManager, clearing ${kvStore.size()} locks" }
        kvStore.clear()
    }
}

/**
 * Configuration for [InMemoryLockManager]
 */
@TaskSchedulingDsl
public class InMemoryLockManagerConfiguration : KeyValueTaskLockManagerConfiguration() {
    /**
     * Lock expiration time in milliseconds.
     * Locks older than this duration will be considered expired and can be re-acquired.
     */
    public var lockExpirationMs: Long = 100

    /**
     * Maximum number of entries allowed in memory before forced cleanup.
     * Default: 10,000 entries
     */
    public var maxSize: Int = 10_000

    /**
     * Probability (0-100) of triggering cleanup on each setIfNotExist call.
     * Higher values = more frequent cleanup but slightly slower performance.
     * Default: 10 (10% of calls trigger cleanup)
     */
    public var cleanupProbability: Int = 10

    override fun createTaskManager(application: Application): InMemoryLockManager =
        InMemoryLockManager(
            name = name.toTaskManagerName(),
            application = application,
            lockExpirationMs = lockExpirationMs,
            maxSize = maxSize,
            cleanupProbability = cleanupProbability,
        )
}

/**
 * Add an [InMemoryLockManager] to the task scheduling configuration.
 *
 * This provides an in-memory task lock manager suitable for single-instance deployments
 * where distributed locking is not required.
 *
 * **Important**: This manager provides locking within a single JVM/process across all
 * InMemoryLockManager instances. It does NOT provide distributed locking across separate
 * JVM processes or machines.
 *
 * **Memory Management**: The in-memory store uses probabilistic cleanup to prevent memory leaks
 * from expired lock entries. Cleanup is triggered:
 * - Always when the number of entries exceeds `maxSize`
 * - Probabilistically on `cleanupProbability`% of lock acquisition attempts
 *
 * Example usage:
 * ```kotlin
 * install(TaskScheduling) {
 *     inMemory {
 *         lockExpirationMs = 60_000      // Locks expire after 60 seconds
 *         maxSize = 10_000               // Trigger cleanup if > 10K entries
 *         cleanupProbability = 10        // 10% chance of cleanup per call
 *     }
 *
 *     task {
 *         name = "my-task"
 *         kronSchedule = { seconds { 0 every 30 } }
 *         task = { executionTime ->
 *             // Task logic here
 *         }
 *     }
 * }
 * ```
 *
 * @param name The name of the task manager. If not provided, this will be the default task manager.
 *             Only one default task manager is allowed.
 * @param config Configuration block for the in-memory lock manager
 */
@TaskSchedulingDsl
public fun TaskSchedulingConfiguration.inMemory(
    name: String? = null,
    config: InMemoryLockManagerConfiguration.() -> Unit = {},
) {
    this.addTaskManager(
        InMemoryLockManagerConfiguration().apply {
            config()
            this.name = name
        },
    )
}

/**
 * In-memory implementation of [KeyValueStore] using a ConcurrentMap.
 *
 * This implementation provides SET-if-not-exists semantics with TTL support
 * using a retry loop and manual expiration checking. The storage is shared
 * across all instances within the same JVM process.
 *
 * **TTL and Memory Management:**
 * - Expired entries are removed lazily when accessed (opportunistic cleanup)
 * - Probabilistic cleanup runs on a percentage of [setIfNotExist] calls
 * - Hard limit cleanup triggers when [maxSize] is exceeded
 *
 * @property maxSize Maximum entries before forced cleanup (default: 10,000)
 * @property cleanupProbability Percentage chance (0-100) of cleanup per call (default: 10)
 */
internal class InMemoryKeyValueStore(
    private val maxSize: Int = 10_000,
    private val cleanupProbability: Int = 10,
) : KeyValueStore {
    private companion object {
        // Shared storage across all InMemoryKeyValueStore instances in the same JVM
        private val storage = ConcurrentMap<String, ExpiringValue>()
    }

    /**
     * Internal wrapper to store values with expiration time
     */
    private data class ExpiringValue(
        val value: String,
        val expiresAtMs: Long,
    )

    override suspend fun setIfNotExist(
        key: String,
        value: String,
        ttlMs: Long,
    ): Boolean {
        // Cleanup strategy: size-based + probabilistic
        when {
            // Hard limit - always cleanup if exceeded
            storage.size > maxSize -> cleanupExpired()

            // Probabilistic cleanup - amortize cost across calls
            cleanupProbability > 0 && Random.nextInt(100) < cleanupProbability ->
                cleanupExpired()
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val expiresAt = now + ttlMs

        while (true) {
            var wasCreated = false
            val expiringValue = ExpiringValue(value, expiresAt)

            val existing =
                storage.computeIfAbsent(key) {
                    wasCreated = true
                    expiringValue
                }

            when {
                // Successfully created new entry
                wasCreated -> return true

                // Entry exists but expired - try to replace atomically
                now >= existing.expiresAtMs -> {
                    storage.remove(key, existing)
                    continue
                }

                // Entry exists and valid - failed to acquire
                else -> return false
            }
        }
    }

    /**
     * Remove all expired entries from storage.
     * Scans entire map - O(n) operation.
     */
    private fun cleanupExpired() {
        val now = Clock.System.now().toEpochMilliseconds()
        val iterator = storage.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now >= entry.value.expiresAtMs) {
                iterator.remove()
            }
        }
    }

    /**
     * Clear all entries from the store
     */
    fun clear(): Unit = storage.clear()

    /**
     * Get the number of entries in the store
     */
    fun size(): Int = storage.size
}
