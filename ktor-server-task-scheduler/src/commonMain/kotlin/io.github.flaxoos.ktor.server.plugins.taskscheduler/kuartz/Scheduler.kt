package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz

import kotlinx.coroutines.Job
import kotlinx.datetime.Instant


// The Scheduler will execute jobs based on their triggers
public class Scheduler(private val jobStore: JobStore, public val clock: () -> Instant) {

    private val runningJobs = mutableListOf<Job>()

    public suspend fun schedule() {
        val currentTime = clock().toEpochMilliseconds()

        // Check all triggers from jobStore, and run the jobs if they should
        jobStore.getAllTriggers().forEach { (job, trigger) ->
            if (trigger.shouldRun(currentTime) && !isJobRunning(job)) {
                runJob(job)
            }
        }
    }

    private suspend fun isJobRunning(job: Job): Boolean {
        // ... check if job is already running
    }

    private suspend fun runJob(job: Job) {

    }
}

// JobStore to store jobs and their triggers
public interface JobStore {
    public suspend fun addJob(jobTrigger: JobTrigger)
    public suspend fun getAllTriggers(): List<JobTrigger>
}

public typealias JobTrigger = Pair<Job, Trigger>
