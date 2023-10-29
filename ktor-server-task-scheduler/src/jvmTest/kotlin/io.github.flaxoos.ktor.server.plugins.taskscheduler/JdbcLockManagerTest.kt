package io.github.flaxoos.ktor.server.plugins.taskscheduler

import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskLockTable
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskLockTable.lock_until
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.TaskLockTable.locked_at
import io.kotest.core.Tuple2
import io.kotest.core.extensions.install
import io.kotest.core.listeners.AfterInvocationListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.testcontainers.ContainerExtension
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class JdbcLockManagerTest : TaskSchedulerPluginTest() {
    private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:13.3"))
    private val postgresContainer = install(ContainerExtension(postgres)) {
        waitingFor(Wait.forListeningPort())
    }

    init {
        register(object : AfterInvocationListener {
            override suspend fun afterInvocation(testCase: TestCase, iteration: Int): Unit {
                transaction {
                    TaskLockTable.deleteAll()
                }
            }
        })

        test("jdbc lock manager") {
            testTaskScheduling(strategy = Database.Jdbc {
                database = org.jetbrains.exposed.sql.Database.connect(
                    url = postgresContainer.getJdbcUrl(), driver = "org.postgresql.Driver",
                    user = postgresContainer.username, password = postgresContainer.password
                ).also {
                    transaction { SchemaUtils.create(TaskLockTable) }
                }
                lockExpirationMs = 5000
            })
        }
    }
}