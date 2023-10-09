package io.github.flaxoos.ktor.server.plugins.taskscheduler

import com.benasher44.uuid.Uuid

sealed class JobKey {
    sealed class Read : JobKey() {
        data class ById(val jobId: Uuid) : Read()
    }

    sealed class Write : JobKey() {
        object Create : Write()
        data class ById(val jobId: Uuid) : Write()
    }

    sealed class Clear : JobKey() {
        object All : Clear()
        data class ById(val jobId: Uuid) : Clear()
    }
}
