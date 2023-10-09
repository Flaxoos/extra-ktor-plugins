package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz

import kotlinx.coroutines.Job
import kotlinx.datetime.Clock


// The Scheduler will execute jobs based on their triggers
class Scheduler(private val jobStore: JobStore) {

    private val runningJobs = mutableListOf<Job>()

    suspend fun schedule() {
        val currentTime = Clock.System.now().toEpochMilliseconds()

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
interface JobStore {
    suspend fun addJob(jobTrigger: JobTrigger)
    suspend fun getAllTriggers(): List<JobTrigger>
}

typealias JobTrigger = Pair<Job, Trigger>
