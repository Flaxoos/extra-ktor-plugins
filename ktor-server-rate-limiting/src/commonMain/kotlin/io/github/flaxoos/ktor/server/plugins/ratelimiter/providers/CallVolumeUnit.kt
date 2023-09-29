package io.github.flaxoos.ktor.server.plugins.ratelimiter.providers

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlin.jvm.JvmInline

 interface CallVolumeUnit {
    val name: String
    val size: Int

    /**
     * Size as Double
     */
    fun size() = size.toDouble()

    /**
     * Size of the given call as measured by this unit
     */
    suspend fun callSize(call: ApplicationCall): Double

    /**
     * Volume is measured in number of calls, with an optional call weighting function to give more weight to a call
     * based on any of it's properties
     */
    open  class Calls(val callWeighting: ApplicationCall.() -> Double = { 1.0 }) : CallVolumeUnit {
        override val name = "calls"
        override val size: Int = 1
        override suspend fun callSize(call: ApplicationCall) = this.callWeighting(call)
    }

    /**
     * Volume is measured in number of bytes of request
     */
    @JvmInline
    value class Bytes(override val size: Int) : CallVolumeUnit {
        override val name: String
            get() = "bytes"

        override suspend fun callSize(call: ApplicationCall) = call.receive(ByteArray::class).size.toDouble()
    }
}
