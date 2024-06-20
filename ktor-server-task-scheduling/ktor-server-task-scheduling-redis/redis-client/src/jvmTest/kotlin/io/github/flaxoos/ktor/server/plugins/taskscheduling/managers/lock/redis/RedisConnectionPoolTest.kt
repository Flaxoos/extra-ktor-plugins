package io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.powermock.reflect.Whitebox

class RedisConnectionPoolTest : FunSpec() {

    init {
        mockkStatic("io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis.RedisConnection_jvmKt")
        val mockConnection = mockk<RedisConnection> {
            coEvery { close() } returns Unit
            coEvery { this@mockk.get("testing") } returns "Ok"
        }
        every { createRedisConnection(any(), any()) } returns mockConnection

        test("should authenticate with username password upon init if username id provided") {
            coEvery { mockConnection.auth(password = "password") } returns true

            RedisConnectionPool(initialConnectionCount = 1, host = "host", port = 0, password = "password")

            coVerify(exactly = 1) { mockConnection.auth(password = "password") }
        }

        test("should authenticate with username password upon init if username and password are provided") {
            coEvery { mockConnection.auth(username = "username", password = "password") } returns true

            RedisConnectionPool(
                initialConnectionCount = 1,
                host = "host",
                port = 0,
                username = "username",
                password = "password"
            )

            coVerify(exactly = 1) { mockConnection.auth(username = "username", password = "password") }
        }

        test("should ping upon init if password is not provided") {
            coEvery { mockConnection.ping("test") } returns "test"

            RedisConnectionPool(initialConnectionCount = 1, host = "host", port = 0)

            coVerify(exactly = 1) { mockConnection.ping("test") }
        }

        test("should acquire and release connection") {
            val pool = RedisConnectionPool(initialConnectionCount = 3, host = "host", port = 0)
            val list = Whitebox.getInternalState<MutableList<RedisConnection>>(pool, "pool")
            val result = pool.withConnection {
                list.size shouldBe 2
                it shouldBe mockConnection
                "Test"
            }

            result shouldBe "Test"
            list.size shouldBe 3
        }

        test("should return null when no connections are available") {
            val initialConnectionCount = 1
            val pool = RedisConnectionPool(initialConnectionCount, host = "host", port = 0)
            val list = Whitebox.getInternalState<MutableList<RedisConnection>>(pool, "pool")

            (0.until(initialConnectionCount + 1)).map {
                launch {
                    val result = pool.withConnection(connectionAcquisitionTimeoutMs = 100) {
                        delay(300)
                        "Test"
                    }
                    result shouldBe if (it < initialConnectionCount) "Test" else null
                }
            }.joinAll()

            list.size shouldBe initialConnectionCount
        }

        test("should add connections up to max connection count") {
            val initialConnectionCount = 1
            val maxConnectionCount = 3
            val pool = RedisConnectionPool(initialConnectionCount, maxConnectionCount, host = "host", port = 0)
            val list = Whitebox.getInternalState<MutableList<RedisConnection>>(pool, "pool")

            (0.until(maxConnectionCount + 1)).map {
                launch {
                    val result = pool.withConnection(connectionAcquisitionTimeoutMs = 100) {
                        delay(300)
                        "Test"
                    }
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

        test("should close all connections") {
            val pool = RedisConnectionPool(initialConnectionCount = 1, host = "host", port = 0)

            pool.closeAll()

            // Check that all connections are closed and removed from the pool
            verify(exactly = 1) { mockConnection.close() }

        }
    }
}

