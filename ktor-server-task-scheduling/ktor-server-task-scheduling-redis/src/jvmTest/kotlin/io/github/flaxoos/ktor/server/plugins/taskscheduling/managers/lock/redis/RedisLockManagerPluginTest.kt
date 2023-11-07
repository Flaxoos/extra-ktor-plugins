package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import com.redis.testcontainers.RedisContainer
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskSchedulingPluginTest
import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.ContainerExtension
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class RedisLockManagerPluginTest : TaskSchedulingPluginTest() {
    private val redis = RedisContainer(DockerImageName.parse("redis:latest"))
        .withEnv("REDIS_USERNAME", "flaxoos")
        .withEnv("REDIS_PASSWORD", "password")
    private val redisContainer = install(ContainerExtension(redis)) {
        waitingFor(Wait.forListeningPort()).withStartupTimeout(1.minutes.toJavaDuration())
    }

    override suspend fun clean() {}

    init {
        context("redis lock manager") {
            testTaskScheduling { freqMs ->
                redis {
                    connectionPoolInitialSize = 1
                    host = redisContainer.host
                    port = redisContainer.firstMappedPort
                    username = "flaxoos"
                    password = "password"
                    connectionAcquisitionTimeoutMs = 1000
                    lockExpirationMs = freqMs.value
                }
            }
        }
    }
}
