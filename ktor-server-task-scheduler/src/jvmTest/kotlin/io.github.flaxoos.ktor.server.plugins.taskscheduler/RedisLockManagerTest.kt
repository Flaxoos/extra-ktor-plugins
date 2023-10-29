package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.redis.testcontainers.RedisContainer
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class RedisLockManagerTest: TaskSchedulerPluginTest(){
    private val redis = RedisContainer(DockerImageName.parse("redis:6.2.6"))
    private val redisContainer = install(ContainerExtension(redis)) {
        waitingFor(Wait.forListeningPort());
    }
    init {
        test("redis lock manager") {
            testTaskScheduling(strategy = Redis {
                connectionPoolSize = 5
                host = redisContainer.host
                port = redisContainer.firstMappedPort
                lockExpirationMs = 5000
                lockAcquisitionTimeoutMs = 1000
                lockAcquisitionRetryFreqMs = 10
            })
        }
    }
}