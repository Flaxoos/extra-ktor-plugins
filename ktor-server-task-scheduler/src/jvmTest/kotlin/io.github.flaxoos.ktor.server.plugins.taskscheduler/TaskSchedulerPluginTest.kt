package io.github.flaxoos.ktor.server.plugins.taskscheduler

import arrow.core.flatten
import dev.inmo.krontab.builder.SchedulerBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
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

abstract class TaskSchedulerPluginTest : FunSpec() {

    suspend fun testTaskScheduling(
        strategy: CoordinationStrategy,
        engineCount: Int = 5,
        executions: Int = 10,
        frequenciesExponentialSeriesInitialMs: Short = 100,
        frequenciesExponentialSeriesN: Short = 3
    ) {
        fun kronTaskSchedule(taskFrequency: Int): SchedulerBuilder.() -> Unit = {
            milliseconds {
                from(0) every taskFrequency
            }
        }

        val frequencies = exponentialScheduleGenerator(
            initial = frequenciesExponentialSeriesInitialMs,
            n = frequenciesExponentialSeriesN
        )
        checkAll(frequencies) { freqMs ->
            coroutineScope {
                val engines = engines(strategy, engineCount, kronTaskSchedule(freqMs))
                    .map { it to launch { it.second.start() } }
                    .also { it.map { engineAndJob -> engineAndJob.second }.joinAll() }
                    .map { it.first }
                delay((freqMs + 50).milliseconds * executions)
                engines.forEach { launch { it.second.stop(0) } }

                engines.map { it.first }.flatten().apply {
                    val list = this.toMutableList()
                    toSet().forEach {
                        list.remove(it)
                    }
                    "Repeated elements:\n${
                        list.toList().distinct().joinToString("\n") { it.format2() }
                    }".asClue {
                        size shouldBeGreaterThanOrEqual executions
                        shouldBeUnique()
                    }
                }
            }
        }
    }

    private fun engines(strategy: CoordinationStrategy, count: Int, kronTaskSchedule: SchedulerBuilder.() -> Unit) =
        (1..count).map { ktorHost ->
            val executions = mutableListOf<DateTime>()
            executions to TestApplicationEngine(
                createTestEnvironment {
                    config = config.mergeWith(MapApplicationConfig("ktor.deployment.host" to ktorHost.toString()))
                    module {
                        install(TaskSchedulerPlugin) {
                            coordinationStrategy = strategy

                            task {
                                name = "Test Kron Task"
                                task = { taskExecutionTime ->
                                    executions.add(taskExecutionTime)
                                    log.info("Host: $ktorHost executing task at ${taskExecutionTime.format2()}")
                                }
                                kronSchedule = kronTaskSchedule
                            }
                        }
                    }
                })
        }

    private fun exponentialScheduleGenerator(initial: Short, n: Short): Exhaustive<Int> {
        val frequencies = (0..<n).map {
            (initial.toDouble().times(if (it == 0) 1.0 else 2.0.pow(it.toDouble()))).toInt()
        }.exhaustive()
        return frequencies
    }
}




