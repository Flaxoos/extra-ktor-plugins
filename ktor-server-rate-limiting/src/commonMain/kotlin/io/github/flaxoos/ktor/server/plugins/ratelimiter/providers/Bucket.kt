package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

sealed class Bucket : RateLimitProvider {
    abstract val coroutineScope: CoroutineScope
    internal abstract val log: KLogger
    protected val volumeChangeJob: Job by lazy {
        log.debug { "Starting bucket job" }
        coroutineScope.launch(jobName) {
            log.debug { "Bucket job started" }
            try {
                while (isActive) {
                    log.debug { "Bucket job waiting $rate" }
                    delay(rate)
                    changeVolume()
                }
            } finally {
                log.debug { "Bucket job stopped" }
            }
        }
    }
    private val jobName = CoroutineName("Bucket job")

    init {
        val maxDuration = Int.MAX_VALUE.milliseconds
        require(rate < maxDuration) {
            "rate must be less than $maxDuration"
        }
    }

    abstract suspend fun changeVolume()

    override fun stop() = volumeChangeJob.cancel()
}
