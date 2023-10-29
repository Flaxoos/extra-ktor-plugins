package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.MongoDbTaskLockKey
import io.kotest.core.extensions.install
import io.kotest.core.listeners.AfterInvocationListener
import io.kotest.core.test.TestCase
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

    init {
        register(object : AfterInvocationListener {
            override suspend fun afterInvocation(testCase: TestCase, iteration: Int): Unit {
                mongoClient.getDatabase("test").getCollection<MongoDbTaskLockKey>("TASK_LOCKS").deleteMany(Document())
            }
        })

        test("mongodb lock manager") {
            testTaskScheduling(strategy = Database.MongoDB {
                databaseName = "test"
                client = mongoClient
                lockExpirationMs = 5000
            })
        }
    }
}