package io.github.flaxoos.ktor.server.plugins.taskscheduling

import dev.inmo.krontab.builder.SchedulerBuilder
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.format2
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import korlibs.time.DateTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

val logger = KotlinLogging.logger { }

private const val ENGINE_COUNT = 5
private const val EXECUTIONS = 4
private const val DEFAULT_EXECUTION_BUFFER_MS = 100
private const val FREQUENCIES_EXPONENTIAL_SERIES_INITIAL_MS = 200.toShort()
private const val FREQUENCIES_EXPONENTIAL_SERIES_N = 3.toShort()
private val concurrencyValues = listOf(1, 3, 6)

abstract class TaskSchedulingPluginTest : FunSpec() {
    protected abstract suspend fun clean()

    suspend fun ContainerScope.testTaskScheduling(
        engineCount: Int = ENGINE_COUNT,
        executions: Int = EXECUTIONS,
        executionBufferMs: Int = DEFAULT_EXECUTION_BUFFER_MS,
        frequenciesExponentialSeriesInitialMs: Short = FREQUENCIES_EXPONENTIAL_SERIES_INITIAL_MS,
        frequenciesExponentialSeriesN: Short = FREQUENCIES_EXPONENTIAL_SERIES_N,
        taskSchedulingConfiguration: TaskSchedulingConfiguration.(TaskFreqMs) -> Unit
    ) {
        fun kronTaskSchedule(taskFrequencyMs: Int): SchedulerBuilder.() -> Unit = {
            milliseconds {
                from(0) every taskFrequencyMs
                this.last
            }
        }

        val frequencies = exponentialScheduleGenerator(
            initial = frequenciesExponentialSeriesInitialMs,
            n = frequenciesExponentialSeriesN
        )
        withData(nameFn = { "Freq: $it ms" }, frequencies) { freqMs ->
            withData(nameFn = { "Concurrency = $it" }, concurrencyValues) { concurrency ->
                coroutineScope {
                    val taskLogsAndEngines = setupApplicationEngines(
                        taskSchedulingConfiguration,
                        engineCount,
                        freqMs.toLong(),
                        concurrency.toShort(),
                        kronTaskSchedule(freqMs)
                    ).map { it to launch { it.second.start() } }
                        .also { it.map { engineAndJob -> engineAndJob.second }.joinAll() }
                        .map { it.first }

                    delay((freqMs + executionBufferMs).milliseconds * executions)
                    taskLogsAndEngines.forEach { launch { it.second.stop(gracePeriodMillis = freqMs * 10L) } }

                    try {
                        with(taskLogsAndEngines.map { it.first }.flatten()) {
                            size shouldBeGreaterThan executions - 2
                            with(groupingBy { it }.eachCount()) {
                                val errors =
                                    this.mapNotNull { if (it.value > concurrency) "${it.key.format2()} was executed ${it.value} times, expected no more than $concurrency" else null }
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

    private fun setupApplicationEngines(
        taskSchedulingConfiguration: TaskSchedulingConfiguration.(TaskFreqMs) -> Unit,
        count: Int,
        freqMs: Long,
        concurrency: Short = 1,
        kronTaskSchedule: SchedulerBuilder.() -> Unit
    ) = (1..count).map { ktorHost ->
        val executionRecords = mutableListOf<DateTime>()
        executionRecords to TestApplicationEngine(
            createTestEnvironment {
                config = config.mergeWith(MapApplicationConfig("ktor.deployment.host" to ktorHost.toString()))
                module {
                    install(TaskScheduling) {
                        taskSchedulingConfiguration(TaskFreqMs(freqMs))

                        task {
                            name = "Test Kron Task"
                            task = { taskExecutionTime ->
                                executionRecords.add(taskExecutionTime)
                                log.debug("Host: $ktorHost executing task at ${taskExecutionTime.format2()}")
                            }
                            kronSchedule = kronTaskSchedule
                            this.concurrency = concurrency.toInt()
                        }
                    }
                }
            }
        )
    }

    private fun exponentialScheduleGenerator(initial: Short, n: Short): List<Int> {
        val frequencies = (0.until(n)).map {
            (initial.toDouble().times(if (it == 0) 1.0 else 2.0.pow(it.toDouble()))).toInt()
        }
        return frequencies
    }
}

@JvmInline
value class TaskFreqMs(val value: Long)
