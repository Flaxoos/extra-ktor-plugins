package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz

// Define a Trigger, when the job should run
public interface Trigger {
    public suspend fun shouldRun(currentTimeMillis: Long): Boolean
}

public class SimpleTrigger(
    private val intervalMillis: Long,
    private var lastRunTime: Long = 0
) : Trigger {
    override suspend fun shouldRun(currentTimeMillis: Long): Boolean {
        return currentTimeMillis - lastRunTime >= intervalMillis
    }
}
