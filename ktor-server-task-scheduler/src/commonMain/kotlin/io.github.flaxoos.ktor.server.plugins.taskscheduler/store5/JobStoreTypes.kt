package io.github.flaxoos.ktor.server.plugins.taskscheduler.store5

import com.benasher44.uuid.Uuid

public sealed class JobKey {
    public sealed class Read : JobKey() {
        public data class ById(val jobId: Uuid) : Read()
    }

    public sealed class Write : JobKey() {
        public data object Create : Write()
        public data class ById(val jobId: Uuid) : Write()
    }

    public sealed class Clear : JobKey() {
        public data object All : Clear()
        public data class ById(val jobId: Uuid) : Clear()
    }
}

public data class JobNetwork(
    val id: Uuid
)

public data class JobCommon(
    val id: Uuid
)

public sealed class JobUpdaterResult {
    public abstract val status: Int

    public sealed class Success : JobUpdaterResult() {
        public data class Ok(override val status: Int) : Success()
        public data class Created(override val status: Int) : Success()
    }

    public sealed class Failure : JobUpdaterResult() {
        public data class ClientError(override val status: Int) : Failure()
        public data class ServerError(override val status: Int) : Failure()
    }
}
