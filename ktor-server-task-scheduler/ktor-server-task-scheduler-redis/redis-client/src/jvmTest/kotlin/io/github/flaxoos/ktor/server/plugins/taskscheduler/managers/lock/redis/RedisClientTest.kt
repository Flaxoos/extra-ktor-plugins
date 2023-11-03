package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers.lock.redis

import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify

class RedisClientTest : FunSpec() {
    init {
        val mockKredsClient = mockk<KredsClient> {
            coEvery { set(any(), any(), any()) } returns "OK"
            coEvery { this@mockk.get(any()) } returns "value"
            coEvery { del(any()) } returns 1L
            every { close() } just Runs
        }

        mockkStatic(::newClient)
        every { newClient(any()) } returns mockKredsClient

        val redisClient = RedisClient("localhost", 6379)

        test("should set a key with expiration and return OK") {
            val result = redisClient.setNx("key", "value", 1000)
            result shouldBe "OK"
            coVerify(exactly = 1) { mockKredsClient.set("key", "value", any()) }

        }

        test("should get a key and return its value") {
            val result = redisClient.get("key")
            result shouldBe "value"
            coVerify(exactly = 1) { mockKredsClient.get("key") }
        }


        test("should delete a key and return 1") {
            val result = redisClient.del("key")
            result shouldBe 1L
            coVerify(exactly = 1) { mockKredsClient.del("key") }

        }

        test("should close the kredsClient") {
            redisClient.close()
            verify(exactly = 1) { mockKredsClient.close() }
        }
    }
}

