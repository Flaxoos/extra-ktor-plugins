package io.github.flaxoos.ktor.server.plugins.taskscheduling

import dev.inmo.krontab.builder.SchedulerBuilder
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.ktor.server.application.log
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.TestApplication
import io.ktor.server.testing.TestApplicationBuilder
import korlibs.time.DateTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

@Suppress("UNUSED")
val logger = KotlinLogging.logger { }

private const val ENGINE_COUNT = 5
private const val EXECUTIONS = 4
private const val DEFAULT_EXECUTION_BUFFER_MS = 100
private const val FREQUENCIES_EXPONENTIAL_SERIES_INITIAL_MS = 200.toShort()
private const val FREQUENCIES_EXPONENTIAL_SERIES_N = 3.toShort()
private val concurrencyValues = listOf(1, 3, 6)
private val taskCounts = listOf(2)

abstract class TaskSchedulingPluginTest : FunSpec() {
    protected abstract suspend fun clean()

    suspend fun ContainerScope.testTaskScheduling(
        engineCount: Int = ENGINE_COUNT,
        executions: Int = EXECUTIONS,
        executionBufferMs: Int = DEFAULT_EXECUTION_BUFFER_MS,
        frequenciesExponentialSeriesInitialMs: Short = FREQUENCIES_EXPONENTIAL_SERIES_INITIAL_MS,
        frequenciesExponentialSeriesN: Short = FREQUENCIES_EXPONENTIAL_SERIES_N,
        taskSchedulingConfiguration: TaskSchedulingConfiguration.(TaskFreqMs) -> Unit,
    ) {
        fun kronTaskSchedule(taskFrequencyMs: Int): SchedulerBuilder.() -> Unit =
            {
                milliseconds {
                    from(0) every taskFrequencyMs
                    this.last
                }
            }

        val frequencies =
            exponentialScheduleGenerator(
                initial = frequenciesExponentialSeriesInitialMs,
                n = frequenciesExponentialSeriesN,
            )
        withData(nameFn = { "Freq: $it ms" }, frequencies) { freqMs ->
            withData(nameFn = { "Task count = $it" }, taskCounts) { taskCount ->
                withData(nameFn = { "Concurrency = $it" }, concurrencyValues) { concurrency ->
                    coroutineScope {
                        val taskLogsAndApplications =
                            setupApplicationEngines(
                                taskSchedulingConfiguration = taskSchedulingConfiguration,
                                count = engineCount,
                                freqMs = freqMs.toLong(),
                                taskCount = taskCount.toShort(),
                                concurrency = concurrency.toShort(),
                                kronTaskSchedule = kronTaskSchedule(freqMs),
                            ).map { it to launch { it.second.start() } }
                                .also { it.map { engineAndJob -> engineAndJob.second }.joinAll() }
                                .map { it.first }

                        delay((freqMs + executionBufferMs).milliseconds * executions)
                        taskLogsAndApplications.forEach { launch { it.second.stop() } }

                        try {
                            with(taskLogsAndApplications.map { it.first }.flatten()) {
                                size shouldBeGreaterThan executions - 2
                                with(groupingBy { it }.eachCount()) {
                                    val errors =
                                        this.mapNotNull {
                                            val expectedExecutions = concurrency * taskCount
                                            if (it.value > expectedExecutions) {
                                                "${it.key.format2()} was executed ${it.value} times, expected no more than $expectedExecutions times"
                                            } else {
                                                null
                                            }
                                        }
                                    if (errors.isNotEmpty()) {
                                        fail(errors.joinToString("\n"))
                                    }
                                }
                            }
                        } finally {
                            delay(1000)
                            clean()
                        }
                    }
                }
            }
        }
    }

    private fun setupApplicationEngines(
        taskSchedulingConfiguration: TaskSchedulingConfiguration.(TaskFreqMs) -> Unit,
        count: Int,
        freqMs: Long,
        taskCount: Short = 1,
        concurrency: Short = 1,
        kronTaskSchedule: SchedulerBuilder.() -> Unit,
    ) = (1..count).map { ktorHost ->
        val executionRecords = mutableListOf<DateTime>()
        val block: TestApplicationBuilder.() -> Unit = {
            environment {
                config = config.mergeWith(MapApplicationConfig("ktor.deployment.host" to ktorHost.toString()))
            }
            install(TaskScheduling) {
                taskSchedulingConfiguration(TaskFreqMs(freqMs))

                for (i in 1 until taskCount + 1) {
                    val taskName = "Test Kron Task: $i"
                    logger.info { "Adding task: $taskName" }
                    task {
                        name = taskName
                        task = { taskExecutionTime ->
                            executionRecords.add(taskExecutionTime)
                            log.info("Host: $ktorHost executing task $taskName at ${taskExecutionTime.format2()}")
                        }
                        kronSchedule = kronTaskSchedule
                        this.concurrency = concurrency.toInt()
                    }
                }
            }
        }
        executionRecords to
            TestApplication {
                engine {
                    shutdownGracePeriod = freqMs * 10
                }
                block()
            }
    }

    private fun exponentialScheduleGenerator(
        initial: Short,
        n: Short,
    ): List<Int> {
        val frequencies =
            (0.until(n)).map {
                (initial.toDouble().times(if (it == 0) 1.0 else 2.0.pow(it.toDouble()))).toInt()
            }
        return frequencies
    }
}

@JvmInline
value class TaskFreqMs(
    val value: Long,
)
