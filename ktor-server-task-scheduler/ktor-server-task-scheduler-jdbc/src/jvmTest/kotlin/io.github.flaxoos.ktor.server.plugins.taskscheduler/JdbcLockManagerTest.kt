package io.github.flaxoos.ktor.server.plugins.taskscheduler

import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.TaskLockTable
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.jdbc
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.jetbrains.exposed.sql.SchemaUtils
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

    override suspend fun clean() {
        transaction { TaskLockTable.deleteAll() }
    }

    init {
        context("jdbc lock manager") {
            testTaskScheduling {
                jdbc {
                    database = org.jetbrains.exposed.sql.Database.connect(
                        url = postgresContainer.getJdbcUrl(),
                        driver = "org.postgresql.Driver",
                        user = postgresContainer.username,
                        password = postgresContainer.password
                    ).also {
                        transaction { SchemaUtils.create(TaskLockTable) }
                    }
                }
            }
        }
    }
}
