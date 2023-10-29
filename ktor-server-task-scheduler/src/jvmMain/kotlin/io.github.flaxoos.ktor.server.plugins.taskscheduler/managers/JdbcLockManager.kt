package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers

import io.github.flaxoos.ktor.server.plugins.taskscheduler.format2
import io.github.flaxoos.ktor.server.plugins.taskscheduler.host
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskLockTable.lock_until
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskLockTable.locked_at
import io.github.flaxoos.ktor.server.plugins.taskscheduler.tasks.Task
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import korlibs.time.DateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DateTimeUnit.Companion.MILLISECOND
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinInstantColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.sql.Connection

private val logger = KotlinLogging.logger {}

internal class JdbcLockManager(
    override val application: Application,
    private val lockExpirationMs: Long,
    private val database: Database
) : DatabaseLockManager<JdbcTaskLockKey>() {
    override suspend fun init(tasks: List<Task>) {
        newSuspendedTransaction(
            application.coroutineContext,
            database,
            transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        ) {
            tasks.forEach { task ->
                TaskLockTable.select {
                    TaskLockTable.id.eq(task.name)
                }.singleOrNull() ?: run {
                    TaskLockTable.insert {
                        it[id] = task.name
                        it[locked_at] = null
                        it[lock_until] = null
                    }
                }
            }
        }
    }

    override suspend fun acquireLock(task: Task, executionTime: DateTime): JdbcTaskLockKey? {
        val taskExecutionInstant = Instant.fromEpochMilliseconds(executionTime.unixMillisLong)
        val key = try {
            newSuspendedTransaction(
                application.coroutineContext,
                database,
                transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            ) {
                if (TaskLockTable.update(
                        where = {
                            (locked_at.isNull() or
                                    lock_until.isNull() or
                                    lock_until.less(LiteralOp(KotlinInstantColumnType(), taskExecutionInstant)))
                        }) {
                        it[locked_at] = taskExecutionInstant
                        it[lock_until] = taskExecutionInstant.plus(lockExpirationMs, MILLISECOND)
                    } == 1
                ) {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Acquired lock for ${task.name}" }
                    JdbcTaskLockKey.get(id = task.name)
                } else {
                    logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name} as no row was updated" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.debug { "${application.host()}: ${executionTime.format2()}: Failed to acquire lock for ${task.name}: ${e.message}" }
            null
        }
        return key
    }

    override suspend fun releaseLock(key: JdbcTaskLockKey) {
        newSuspendedTransaction(application.coroutineContext, database) {
            TaskLockTable.update({
                TaskLockTable.id.eq(key.name)
            }) {
                it[locked_at] = null
                it[lock_until] = null
            }
        }
    }
}

public class JdbcTaskLockKey(
    id: EntityID<String>,
) : Entity<String>(id), DatabaseTaskLockKey {
    public companion object : EntityClass<String, JdbcTaskLockKey>(TaskLockTable)

    override val name: String = id.value
    private var locked_at: Instant? by TaskLockTable.locked_at
    private var lock_until: Instant? by TaskLockTable.lock_until

    override val lockedAt: DateTime?
        get() = locked_at?.let { DateTime(it.toEpochMilliseconds()) }
    override val lockUntil: DateTime?
        get() = lock_until?.let { DateTime(it.toEpochMilliseconds()) }

    override fun toString(): String =
        "name=$name, lockedAt=${lockedAt?.format2()}, lockUntil=${lockUntil?.format2()}"

}

internal object TaskLockTable : IdTable<String>("TASK_LOCKS") {
    override val id: Column<EntityID<String>> = varchar("NAME", 64).uniqueIndex().entityId()
    val locked_at = timestamp("LOCKED_AT").nullable()
    val lock_until = timestamp("LOCK_UNTIL").nullable()
}
