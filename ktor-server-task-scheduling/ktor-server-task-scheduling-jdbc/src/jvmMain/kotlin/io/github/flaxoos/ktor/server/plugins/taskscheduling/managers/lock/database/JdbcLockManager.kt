package util

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingDsl
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManagerConfiguration.TaskManagerName.Companion.toTaskManagerName
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DatabaseTaskLock
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DatabaseTaskLockManager
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DatabaseTaskLockManagerConfiguration
import io.github.flaxoos.ktor.server.plugins.taskscheduling.tasks.Task
import io.ktor.server.application.*
import korlibs.time.DateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.properties.Delegates
import kotlin.time.Instant

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
        suspendTransaction { SchemaUtils.create(taskLockTable) }
    }

    override suspend fun insertTaskLock(
        task: Task,
        taskConcurrencyIndex: Int,
    ): Boolean =
        suspendTransaction(
            database,
            transactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED,
        ) {
            maxAttempts = 1
            taskLockTable.insertIgnore {
                it[name] = task.name
                it[concurrencyIndex] = taskConcurrencyIndex
                it[lockedAt] = null
            }
        }.insertedCount == 1

    override suspend fun updateTaskLock(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime,
    ): JdbcTaskLock? =
        suspendTransaction(
            db = database,
            transactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED,
        ) {
            taskLockTable.update(
                where = {
                    selectClause(
                        task,
                        concurrencyIndex,
                        executionTime,
                    )
                },
            ) {
                it[lockedAt] = executionTime.toInstant()
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

    override fun close() {}

    private fun selectClause(
        task: Task,
        concurrencyIndex: Int,
        executionTime: DateTime,
    ) = (
            taskLockTable.name eq task.name and
                    taskLockTable.concurrencyIndex.eq(concurrencyIndex)
            ) and (taskLockTable.lockedAt.isNull() or taskLockTable.lockedAt.less(executionTime.toInstant()))
}

public class JdbcTaskLock(
    override val name: String,
    override val concurrencyIndex: Int,
    override val lockedAt: DateTime,
) : DatabaseTaskLock {
    override fun toString(): String = "name=$name, concurrencyIndex=$concurrencyIndex, lockedAt=${lockedAt.format2()}}"
}

public abstract class ExposedTaskLockTable(
    tableName: String,
) : Table(tableName) {
    public abstract val name: Column<String>
    public abstract val concurrencyIndex: Column<Int>
    public abstract val lockedAt: Column<Instant?>
}

public object DefaultTaskLockTable : ExposedTaskLockTable("task_locks") {
    override val name: Column<String> = text("_name")
    override val concurrencyIndex: Column<Int> = integer("concurrency_index")
    override val lockedAt: Column<Instant?> = timestamp("locked_at").nullable().index()

    override val primaryKey: PrimaryKey = PrimaryKey(firstColumn = name, concurrencyIndex, name = "pk_task_locks")
}

@TaskSchedulingDsl
public class JdbcJobLockManagerConfiguration : DatabaseTaskLockManagerConfiguration() {
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

private fun DateTime.toInstant() = Instant.fromEpochMilliseconds(unixMillisLong)
