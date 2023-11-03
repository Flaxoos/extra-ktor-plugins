package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database

import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.TaskSchedulerDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManager
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.TaskLockTable.concurrencyIndex
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.TaskLockTable.lockedAt
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.TaskLockTable.name
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import korlibs.time.DateTime.Companion.fromUnixMillis
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinInstantColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.sql.Connection
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}

internal class JdbcLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    private val database: Database
) : DatabaseTaskLockManager<JdbcTaskLockKey>() {
    override suspend fun init(tasks: List<Task>) {
        logger.debug { "Initializing JdbcLockManager for ${tasks.size} tasks" }
        tasks.forEach { task ->
            task.concurrencyRange().forEach { taskConcurrencyIndex ->
                insertTaskLock(task, taskConcurrencyIndex)
            }
        }
    }

    override suspend fun acquireLock(task: Task, executionTime: DateTime, concurrencyIndex: Int): JdbcTaskLockKey? {
        val taskExecutionInstant = Instant.fromEpochMilliseconds(executionTime.unixMillisLong)
        val updateResult = updateTaskLock(task, concurrencyIndex, taskExecutionInstant, executionTime)
        return if (updateResult == 1) {
            logger.debug {
                "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name} - $concurrencyIndex"
            }
            JdbcTaskLockKey(
                name = task.name,
                concurrencyIndex = concurrencyIndex,
                lockedAt = executionTime
            )
        } else {
            logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} - $concurrencyIndex as no row was updated" }
            null
        }
    }

    override fun close() {}

    private suspend fun insertTaskLock(
        task: Task,
        taskConcurrencyIndex: Int
    ) {
        newSuspendedTransaction(
            application.coroutineContext,
            database,
            transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        ) {
            runCatching {
                repetitionAttempts = 0
                debug = true
                TaskLockTable.insertIgnore {
                    it[name] = task.name
                    it[concurrencyIndex] = taskConcurrencyIndex
                    it[lockedAt] = Instant.fromEpochMilliseconds(0)
                }
            }.onFailure {
                logger.error(it) { "Failed to initialize JdbcLockManager" }
            }.getOrNull()
        }?.let {
            if (it.insertedCount > 0) {
                logger.debug { "JdbcLockManager inserted ${it.insertedCount} tasks during initialization" }
            }
        }
    }

    private suspend fun updateTaskLock(
        task: Task,
        concurrencyIndex: Int,
        taskExecutionInstant: Instant,
        executionTime: DateTime
    ) = newSuspendedTransaction(
        application.coroutineContext,
        db = database,
        transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        runCatching {
            TaskLockTable.update(
                where = {
                    selectClause(
                        task,
                        concurrencyIndex,
                        taskExecutionInstant
                    )
                }
            ) {
                it[lockedAt] = taskExecutionInstant
                it[TaskLockTable.concurrencyIndex] = concurrencyIndex
            }
        }.onFailure {
            logger.warn { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} - $concurrencyIndex: ${it.message}" }
        }.getOrNull()
    }

    private fun selectClause(
        task: Task,
        concurrencyIndex: Int,
        taskExecutionInstant: Instant
    ) =
        (TaskLockTable.name eq task.name and TaskLockTable.concurrencyIndex.eq(concurrencyIndex)) and
            lockedAt.neq(LiteralOp(KotlinInstantColumnType(), taskExecutionInstant))

    override suspend fun releaseLock(key: JdbcTaskLockKey) {}
}

public class JdbcTaskLockKey(
    override val name: String,
    override val concurrencyIndex: Int,
    override val lockedAt: DateTime
) : DatabaseTaskLockKey {
    override fun toString(): String =
        "name=$name, concurrencyIndex=$concurrencyIndex, lockedAt=${lockedAt.format2()}}"
}

internal object TaskLockTable : Table("task_locks") {
    val name = text("_name")
    val concurrencyIndex = integer("concurrency_index")
    val lockedAt = timestamp("locked_at").index()

    override val primaryKey: PrimaryKey = PrimaryKey(firstColumn = name, concurrencyIndex, name = "pk_task_locks")
}

private fun ResultRow.toJdbcTaskLockKey(): JdbcTaskLockKey = JdbcTaskLockKey(
    name = this[name],
    concurrencyIndex = this[concurrencyIndex],
    lockedAt = fromUnixMillis(this[lockedAt].toEpochMilliseconds())
)

@TaskSchedulerDsl
public class JdbcJobLockManagerConfiguration : DatabaseTaskLockManagerConfiguration() {
    public var database: Database by Delegates.notNull()
    override fun createTaskManager(application: Application): TaskManager<*> =
        JdbcLockManager(
            name = name.toTaskManagerName(),
            application = application,
            database = database
        )
}

@TaskSchedulerDsl
public fun TaskSchedulerConfiguration.jdbc(
    name: String? = null,
    config: JdbcJobLockManagerConfiguration.() -> Unit
) {
    this.addTaskManager { application ->
        JdbcJobLockManagerConfiguration().apply {
            config()
            this.name = name
        }.createTaskManager(application)
    }
}
