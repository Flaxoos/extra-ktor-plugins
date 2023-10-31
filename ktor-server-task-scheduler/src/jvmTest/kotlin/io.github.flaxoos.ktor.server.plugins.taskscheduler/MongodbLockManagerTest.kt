package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.mongodb.MongoDbTaskLockKey
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.database.mongodb.mongoDb
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class MongodbLockManagerTest : TaskSchedulerPluginTest() {
    private val mongodb = MongoDBContainer(DockerImageName.parse("mongo:6.0.4"))
    private val mongodbContainer = install(ContainerExtension(mongodb)) {
        waitingFor(Wait.forListeningPort())
    }
    val mongoClient = MongoClient.create(mongodbContainer.connectionString)

    override suspend fun clean() {
        mongoClient.getDatabase("test").getCollection<MongoDbTaskLockKey>("TASK_LOCKS").deleteMany(Document())
    }

    init {
        context("mongodb lock manager") {
            testTaskScheduling {
                mongoDb {
                    databaseName = "test"
                    client = mongoClient
                }
            }
        }
    }
}