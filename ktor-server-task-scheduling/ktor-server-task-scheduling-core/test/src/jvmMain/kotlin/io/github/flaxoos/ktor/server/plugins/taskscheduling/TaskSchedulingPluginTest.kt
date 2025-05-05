package io.github.flaxoos.ktor.server.plugins.taskscheduling

import dev.inmo.krontab.builder.SchedulerBuilder
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.TaskManager.Companion.formatTime
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.inspectors.forAll
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.ktor.server.application.log
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.TestApplication
import io.ktor.server.testing.TestApplicationBuilder
import korlibs.time.DateTime
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

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
                        val taskExecutionsAndApplications =
                            setupApplicationEngines(
                                taskSchedulingConfiguration = taskSchedulingConfiguration,
                                count = engineCount,
                                freqMs = freqMs.toLong(),
                                taskCount = taskCount.toShort(),
                                concurrency = concurrency.toShort(),
                                kronTaskSchedule = kronTaskSchedule(freqMs),
                            ).map { executionsAndApp ->
                                executionsAndApp to
                                    launch {
                                        val app = executionsAndApp.second
                                        app.start()
                                    }
                            }.also { appsAndJobs ->
                                // wait for all app engines to start
                                appsAndJobs.map { appAndJob -> appAndJob.second }.joinAll()
                            }.map {
                                // don't need to remember the job
                                it.first
                            }

                        delay(freqMs.milliseconds * executions)
                        delay(executionBufferMs.milliseconds)
                        taskExecutionsAndApplications
                            .map { (_, app) ->
                                launch {
                                    app.stop()
                                }
                            }.joinAll()

                        val totalExecutions = taskExecutionsAndApplications.sumOf { it.first.size }
                        val totalExpectedExecutionsMinusLastRound = taskCount * concurrency * (executions - 1)
                        totalExecutions shouldBeGreaterThan totalExpectedExecutionsMinusLastRound

                        try {
                            taskExecutionsAndApplications.map { it.first }.flatten().let { records ->
                                records
                                    .groupBy { it.taskName to it.executionTime }
                                    .toSortedMap { (_, a), (_, b) ->
                                        a.compareTo(b)
                                    }.let { sortedRecords ->
                                        val (_, lastTime) = sortedRecords.lastKey()
                                        sortedRecords.forAll { (pair, executions) ->
                                            val (taskName, executionTime) = pair
                                            withClue(
                                                "\n$taskName - ${executionTime.formatTime()} was executed ${executions.size} times instead of $concurrency: \n\t${
                                                    records.filter { it.taskName == taskName && it.executionTime == executionTime }
                                                        .joinToString("\n\t")
                                                }",
                                            ) {
                                                if (executionTime == lastTime) {
                                                    // The last round might miss executions due to the server shutting down
                                                    executions.size shouldBeLessThanOrEqual concurrency
                                                } else {
                                                    executions.size shouldBe concurrency
                                                }
                                            }
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

    data class ExecutionRecord(
        val taskName: String,
        val ktorHost: String,
        val executionTime: DateTime,
    ) {
        override fun toString(): String =
            "ExecutionRecord(taskName='$taskName', ktorHost='$ktorHost', executionTime=${executionTime.formatTime()})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExecutionRecord) return false

            if (taskName != other.taskName) return false
            if (executionTime != other.executionTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = taskName.hashCode()
            result = 31 * result + executionTime.hashCode()
            return result
        }
    }

    private fun setupApplicationEngines(
        taskSchedulingConfiguration: TaskSchedulingConfiguration.(TaskFreqMs) -> Unit,
        count: Int,
        freqMs: Long,
        taskCount: Short = 1,
        concurrency: Short = 1,
        kronTaskSchedule: SchedulerBuilder.() -> Unit,
    ): List<Pair<List<ExecutionRecord>, TestApplication>> =
        (1..count).map { ktorHost ->
            val executionRecords = mutableListOf<ExecutionRecord>()
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
                                executionRecords.add(ExecutionRecord(taskName, ktorHost.toString(), taskExecutionTime))
                                log.info("Host: $ktorHost executing task $taskName at ${taskExecutionTime.formatTime()}")
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
