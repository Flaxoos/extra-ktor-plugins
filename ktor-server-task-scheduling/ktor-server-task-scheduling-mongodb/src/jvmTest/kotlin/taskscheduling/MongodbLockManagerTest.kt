package io.github.flaxoos.ktor.server.plugins.taskscheduling

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.MongoDbTaskLock
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class MongodbLockManagerTest : TaskSchedulingPluginTest() {
    private val mongodb = MongoDBContainer(DockerImageName.parse("mongo:6.0.4"))
    private val mongodbContainer =
        install(ContainerExtension(mongodb)) {
            waitingFor(Wait.forListeningPort())
        }
    private val mongoClient = MongoClient.create(mongodbContainer.connectionString)

    override suspend fun clean() {
        mongoClient.getDatabase("test").getCollection<MongoDbTaskLock>("TASK_LOCKS").deleteMany(Document())
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
