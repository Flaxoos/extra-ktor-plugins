package io.github.flaxoos.ktor.server.plugins.taskscheduler.store5

import com.benasher44.uuid.Uuid

sealed class JobKey {
    sealed class Read : JobKey() {
        data class ById(val jobId: Uuid) : Read()
    }

    sealed class Write : JobKey() {
        data object Create : Write()
        data class ById(val jobId: Uuid) : Write()
    }

    sealed class Clear : JobKey() {
        data object All : Clear()
        data class ById(val jobId: Uuid) : Clear()
    }
}

data class JobNetwork(
    val id: Uuid
)

data class JobCommon(
    val id: Uuid
)

sealed class JobUpdaterResult {
    abstract val status: Int

    sealed class Success : JobUpdaterResult() {
        data class Ok(override val status: Int) : Success()
        data class Created(override val status: Int) : Success()
    }

    sealed class Failure : JobUpdaterResult() {
        data class ClientError(override val status: Int) : Failure()
        data class ServerError(override val status: Int) : Failure()
    }
}
