package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.github.flaxoos.common.queueList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class RedisConnectionPoolTest : FunSpec() {

    init {
        mockkStatic("io.github.flaxoos.common.Collections_jvmKt")
        val list = mutableListOf<RedisConnection>()
        every { queueList<RedisConnection>() } returns list

        mockkStatic("io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis.RedisConnection_jvmKt")
        val mockConnection = mockk<RedisConnection> { coEvery { close() } returns Unit }
        every { createRedisConnection(any(), any()) } returns mockConnection

        afterTest {
            list.clear()
        }

        test("should acquire and release connection") {
            val pool = RedisConnectionPool(initialConnectionCount = 3, host = "host", port = 0)

            val result = pool.withConnection {
                list.size shouldBe 2
                it shouldBe mockConnection
                "Test"
            }

            result shouldBe "Test"
            list.size shouldBe 3
        }

        test("should add connections up to max connection count") {
            val initialConnectionCount = 1
            val maxConnectionCount = 3
            val pool = RedisConnectionPool(initialConnectionCount, maxConnectionCount, host = "host", port = 0)

            (0.until(maxConnectionCount+1)).map {
                launch {
                    val result = pool.withConnection(connectionAcquisitionTimeoutMs = 100) {
                        delay(300)
                        "Test"
                    }
                    println(result)
                    result shouldBe if (it < maxConnectionCount) "Test" else null
                }
            }.joinAll()

            list.size shouldBe 3
        }

        test("should retry acquiring connection") {
            val size = 2
            val pool = RedisConnectionPool(initialConnectionCount = size, host = "host", port = 0)

            val results = (0.until(size)).map {
                pool.withConnection(100) {
                    "Test"
                }
            }

            results.size shouldBe size
            results shouldNotContain null
        }

        test("should timeout acquiring connection") {
            val size = 2
            val pool = RedisConnectionPool(initialConnectionCount = size - 1, host = "host", port = 0)
            val timeout = 100L
            val results = (0.until(size)).map {
                async {
                    pool.withConnection(timeout) {
                        delay(timeout)
                        "Test"
                    }
                }
            }.awaitAll()

            results.size shouldBe size
            results shouldContainExactlyInAnyOrder listOf("Test", null)
        }

        test("should return null when no connections are available") {
            val pool = RedisConnectionPool(initialConnectionCount = 0, host = "host", port = 0)

            val result = pool.withConnection { "Test" }

            result shouldBe null
            list.size shouldBe 0
        }

        test("should close all connections") {
            val pool = RedisConnectionPool(initialConnectionCount = 1, host = "host", port = 0)

            pool.closeAll()

            // Check that all connections are closed and removed from the pool
            verify(exactly = 1) { mockConnection.close() }

        }
    }
}

