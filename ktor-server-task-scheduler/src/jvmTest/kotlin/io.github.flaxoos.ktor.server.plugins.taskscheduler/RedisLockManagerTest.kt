package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.redis.testcontainers.RedisContainer
import io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis.redis
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class RedisLockManagerTest : TaskSchedulerPluginTest() {
    private val redis = RedisContainer(DockerImageName.parse("redis:6.2.6"))
    private val redisContainer = install(ContainerExtension(redis)) {
        waitingFor(Wait.forListeningPort()).withStartupTimeout(1.minutes.toJavaDuration())
    }

    override suspend fun clean() {}

    init {
        context("redis lock manager") {
            testTaskScheduling {
                redis {
                    connectionPoolSize = 5
                    host = redisContainer.host
                    port = redisContainer.firstMappedPort
                    lockAcquisitionTimeoutMs = 1000
                    lockAcquisitionRetryFreqMs = 10
                }
            }
        }
    }
}