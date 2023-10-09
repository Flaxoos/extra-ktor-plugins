package io.github.flaxoos.ktor.server.plugins.taskscheduler.kuartz


import com.benasher44.uuid.Uuid
import io.ktor.utils.io.reader
import io.ktor.utils.io.writer
import kotlinx.coroutines.Job
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder

interface JobLockManager {
    suspend fun acquireLock(job: Job): Boolean
    suspend fun releaseLock(job: Job)
}

class JobLockStoreManager<Network : Any, Local : Any, Converter, Validator, Updater>(
    fetch: suspend (key: Uuid) -> Network
) {
    init {
        StoreBuilder
            .from(
                fetcher = Fetcher.of {
                    fetch
                },
                sourceOfTruth = SourceOfTruth.of(
                    reader = {},
                    writer = {},
                    delete = {},
                    deleteAll = {}
    }
}

class DatabaseJobLockManager : JobLockManager {
    // Implement using SQL transactions to ensure atomicity.
}

class JobLockScheduler(private val jobStore: JobStore, private val lockManager: JobLockManager) {
    //...
    private suspend fun runJob(job: Job) {
        if (lockManager.acquireLock(job)) {
            // Execute job and release lock afterward
            job.start()
            lockManager.releaseLock(job)
        }
    }
}
