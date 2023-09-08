package flaxoos.github.io

import flaxoos.github.io.plugins.configureKafka
import flaxoos.github.io.plugins.configureRouting
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerName.Companion.toCircuitBreakerName
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreaking
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.global
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.register
import io.github.flaxoos.ktor.server.plugins.ratelimiter.ApplicationRateLimiting
import io.github.flaxoos.ktor.server.plugins.ratelimiter.KtorServerPluginUnstableAPI
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun Application.module() {
    configureRouting()
    configureKafka()

    @OptIn(KtorServerPluginUnstableAPI::class)
    install(ApplicationRateLimiting) {
        limit = 10
        timeWindow = 1.seconds
    }
}

val client = HttpClient {
    install(CircuitBreaking) {
        global {
            failureThreshold = 20
            halfOpenFailureThreshold = 5
            resetInterval = 1.seconds
            failureTrigger = {
                status.value >= 400
            }
        }
        register("my-circuit-breaker".toCircuitBreakerName()) {
            failureThreshold = 10
            halfOpenFailureThreshold = 2
            resetInterval = 2.seconds
        }
    }
}
