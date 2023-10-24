package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.benasher44.uuid.uuid4
import com.redis.testcontainers.RedisContainer
import dev.inmo.krontab.builder.SchedulerBuilder
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.shouldBe
import io.ktor.server.application.install
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class TaskSchedulerPluginTest : FunSpec() {


    init {
//        coroutineTestScope = true

        val redis = RedisContainer(DockerImageName.parse("redis:6.2.6"))
        val redisContainer = install(ContainerExtension(redis)) {
            waitingFor(Wait.forListeningPort());
        }

        test("Test RedisLockManager") {
            val replication = 1
            val taskChannel = Channel<String> {}
            val freqMs = 100
            val intervalTaskSchedule = freqMs.milliseconds
            val kronTaskSchedule: SchedulerBuilder.() -> Unit = {
                milliseconds {
                    from(0) every freqMs
                }
            }
            val intervalTaskId = uuid4()
            val kronTaskId = uuid4()
            val engines = (1..replication).map { replica ->
                TestApplicationEngine(
                    createTestEnvironment {
                        module {
                            install(TaskSchedulerPlugin) {
                                coordinationStrategy = CoordinationStrategy.Lock.Redis {
                                    connectionPoolSize = 5
                                    host = redisContainer.host
                                    port = redisContainer.firstMappedPort
                                    expiresMs = 1000
                                    timeoutMs = 1000
                                    lockAcquisitionRetryFreqMs = 10
                                }

                                intervalTask {
                                    id = intervalTaskId
                                    name = "Test Interval Task"
                                    task = {
                                        log.info("$replica executing task")
                                        taskChannel.send("interval-$replica")
                                    }
                                    schedule = intervalTaskSchedule
                                }

//                                kronTask {
//                                    id = kronTaskId
//                                    name = "Test Kron Task"
//                                    task = {
//                                        taskChannel.send("kron-$it")
//                                    }
//                                    kronSchedule = kronTaskSchedule
//                                }
                            }
                        }
                    })
            }

            engines.map { launch { it.start() } }.joinAll()
            this.testCoroutineScheduler.advanceTimeBy((freqMs * 5L) + (freqMs / 2))
            engines.forEach { launch { it.stop(0) } }

            val taskMessages = taskChannel.toList()
            taskMessages.size shouldBe replication
        }
    }
}