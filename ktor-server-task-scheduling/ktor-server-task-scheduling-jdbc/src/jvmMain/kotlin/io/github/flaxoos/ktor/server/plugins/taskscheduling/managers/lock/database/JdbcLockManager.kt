package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DefaultTaskLockTable.lockedAt
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DefaultTaskLockTable.name
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinInstantColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.Connection
import kotlin.properties.Delegates

/**
 * An implementation of [DatabaseTaskLockManager] using JDBC and Exposed as the lock store
 * The manager will take care of generating the lock table using the [SchemaUtils] and the [DefaultTaskLockTable].
 * the schema utils should handle the case where the table already exists. TODO: test this
 * Alternatively, you can use implement the [ExposedTaskLockTable] yourself and provide it instead
 */
public class JdbcLockManager(
    override val name: TaskManagerConfiguration.TaskManagerName,
    override val application: Application,
    /**
     * An exposed database to use
     */
    private val database: Database,
    /**
     * The task lock table to use, if not provided, the [DefaultTaskLockTable] will be used
     */
    private val taskLockTable: ExposedTaskLockTable = DefaultTaskLockTable,
) : DatabaseTaskLockManager<JdbcTaskLock>() {

    override suspend fun initTaskLockTable() {
        transaction { SchemaUtils.create(taskLockTable) }
    }

    override suspend fun insertTaskLock(
        task: Task,
        taskConcurrencyIndex: Int,
    ): Boolean =
        newSuspendedTransaction(
            application.coroutineContext,
            database,
            transactionIsolation = Connection.TRANSACTION_READ_COMMITTED,
        ) {
            repetitionAttempts = 0
            debug = true
            taskLockTable.insertIgnore {
                it[name] = task.name
                it[concurrencyIndex] = taskConcurrencyIndex
                it[lockedAt] = Instant.fromEpochMilliseconds(0)
            }
        }.insertedCount == 1

    override suspend fun updateTaskLock(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime,
    ): JdbcTaskLock? = newSuspendedTransaction(
        application.coroutineContext,
        db = database,
        transactionIsolation = Connection.TRANSACTION_READ_COMMITTED,
    ) {
        val taskExecutionInstant = Instant.fromEpochMilliseconds(executionTime.unixMillisLong)
        taskLockTable.update(
            where = {
                selectClause(
                    task,
                    concurrencyIndex,
                    taskExecutionInstant,
                )
            },
        ) {
            it[lockedAt] = taskExecutionInstant
            it[taskLockTable.concurrencyIndex] = concurrencyIndex
        }
    }.let {
        if (it == 1) {
            JdbcTaskLock(
                name = task.name,
                concurrencyIndex = concurrencyIndex,
                lockedAt = executionTime,
            )
        } else {
            null
        }
    }

    override suspend fun releaseLockKey(key: JdbcTaskLock) {}

    override fun close() {}

    private fun selectClause(
        task: Task,
        concurrencyIndex: Int,
        taskExecutionInstant: Instant,
    ) =
        (taskLockTable.name eq task.name and taskLockTable.concurrencyIndex.eq(concurrencyIndex)) and
            lockedAt.neq(LiteralOp(KotlinInstantColumnType(), taskExecutionInstant))
}

public class JdbcTaskLock(
    override val name: String,
    override val concurrencyIndex: Int,
    override val lockedAt: DateTime,
) : DatabaseTaskLock {
    override fun toString(): String =
        "name=$name, concurrencyIndex=$concurrencyIndex, lockedAt=${lockedAt.format2()}}"
}

public abstract class ExposedTaskLockTable(tableName: String) : Table(tableName) {
    public abstract val name: Column<String>
    public abstract val concurrencyIndex: Column<Int>
    public abstract val lockedAt: Column<Instant>
}

public object DefaultTaskLockTable : ExposedTaskLockTable("task_locks") {
    override val name: Column<String> = text("_name")
    override val concurrencyIndex: Column<Int> = integer("concurrency_index")
    override val lockedAt: Column<Instant> = timestamp("locked_at").index()

    override val primaryKey: PrimaryKey = PrimaryKey(firstColumn = name, concurrencyIndex, name = "pk_task_locks")
}

@TaskSchedulingDsl
public class JdbcJobLockManagerConfiguration : DatabaseTaskLockManagerConfiguration<JdbcTaskLock>() {
    public var database: Database by Delegates.notNull()
    override fun createTaskManager(application: Application): JdbcLockManager =
        JdbcLockManager(
            name = name.toTaskManagerName(),
            application = application,
            database = database,
        )
}

/**
 * Add a [JdbcLockManager]
 */
@TaskSchedulingDsl
public fun TaskSchedulingConfiguration.jdbc(

    /**
     * The name of the task manager, will be used to identify the task manager when assigning tasks to it
     * if none is provided, it will be considered the default one. only one default task manager is allowed.
     */
    name: String? = null,
    config: JdbcJobLockManagerConfiguration.() -> Unit,
) {
    this.addTaskManager(
        JdbcJobLockManagerConfiguration().apply {
            config()
            this.name = name
        },
    )
}
