package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueStore
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueTaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.keyvalue.KeyValueTaskLockManagerConfiguration
import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking

/**
 * An implementation of [KeyValueTaskLockManager] using Redis as the lock store
 */
public class RedisLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    private val connectionPool: RedisConnectionPool,
    lockExpirationMs: Long,
    connectionAcquisitionTimeoutMs: Long,
) : KeyValueTaskLockManager(lockExpirationMs) {
    override val store: KeyValueStore = RedisKeyValueStore(connectionPool, connectionAcquisitionTimeoutMs)

    override fun close() {
        runBlocking {
            connectionPool.closeAll()
        }
    }
}

@TaskSchedulingDsl
public class RedisTaskLockManagerConfiguration(
    /**
     * The redis host
     */
    public var host: String = "undefined",
    /**
     * The redis port
     */
    public var port: Int = 0,
    /**
     * The redis username
     */
    public var username: String? = null,
    /**
     * The redis password
     */
    public var password: String? = null,
    /**
     * For how long the lock should be valid, effectively, the pxMilliseconds for the setNx command
     */
    public var lockExpirationMs: Long = 100,
    /**
     * How many connections should the pool have initially
     */
    public var connectionPoolInitialSize: Int = 10,
    /**
     * The maximum number of connections in the pool
     */
    public var connectionPoolMaxSize: Int = connectionPoolInitialSize,
    /**
     * The timeout for trying to get a connection to from the pool
     */
    public var connectionAcquisitionTimeoutMs: Long = 100,
) : KeyValueTaskLockManagerConfiguration() {
    override fun createTaskManager(application: Application): RedisLockManager =
        RedisLockManager(
            name = name.toTaskManagerName(),
            application = application,
            connectionPool =
                RedisConnectionPool(
                    initialConnectionCount = connectionPoolInitialSize,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                ),
            lockExpirationMs = lockExpirationMs,
            connectionAcquisitionTimeoutMs = connectionAcquisitionTimeoutMs,
        )
}

/**
 * Add a [RedisLockManager]
 */
@TaskSchedulingDsl
public fun TaskSchedulingConfiguration.redis(
    /**
     * The name of the task manager, will be used to identify the task manager when assigning tasks to it
     * if none is provided, it will be considered the default one. only one default task manager is allowed.
     */
    name: String? = null,
    config: RedisTaskLockManagerConfiguration.() -> Unit,
) {
    this.addTaskManager(
        RedisTaskLockManagerConfiguration().apply {
            config()
            this.name = name
        },
    )
}

/**
 * Redis implementation of [KeyValueStore] using Redis's SET NX command
 */
internal class RedisKeyValueStore(
    private val connectionPool: RedisConnectionPool,
    private val connectionAcquisitionTimeoutMs: Long,
) : KeyValueStore {
    override suspend fun setIfNotExist(
        key: String,
        value: String,
        ttlMs: Long,
    ): Boolean =
        connectionPool.withConnection(connectionAcquisitionTimeoutMs) { redisConnection ->
            redisConnection.setNx(key, value, ttlMs) != null
        } ?: false
}
