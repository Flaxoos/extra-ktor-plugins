package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz

// Define a Trigger, when the job should run
interface Trigger {
    suspend fun shouldRun(currentTimeMillis: Long): Boolean
}

// Example of a SimpleTrigger
class SimpleTrigger(
    private val intervalMillis: Long,
    private var lastRunTime: Long = 0
) : Trigger {
    override suspend fun shouldRun(currentTimeMillis: Long): Boolean {
        return currentTimeMillis - lastRunTime >= intervalMillis
    }
}
