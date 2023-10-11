package io.github.flaxoos.ktor.server.plugins.taskscheduler.store5

import com.benasher44.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.OnUpdaterCompletion
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

fun provide(
    api: JobApi
): Fetcher<JobKey, JobNetwork> = Fetcher.of { key ->
    require(key is JobKey.Read)
    when (key) {
        is JobKey.Read.ById -> api.getJobById(key.jobId)
    }
}

fun provide(
    api: JobApi
): Updater<JobKey, JobCommon, JobUpdaterResult> =
    Updater.by(
        post = { key, input ->
            require(key is JobKey.Write)
            when (key) {
                is JobKey.Write.Create -> api.create(input)
                is JobKey.Write.ById -> api.update(key.jobId, input)
            }
        },
        onCompletion = OnUpdaterCompletion(
            onSuccess = { success: UpdaterResult.Success ->
                UserLogger.post(StoreEvents.Update(success.status))
            },
            onFailure = { failure: UpdaterResult.Error ->
                UserLogger.post(StoreEvents.Update(failure.status))
            }
        )
    )

fun provide(
    db: JobDb
): SourceOfTruth<JobKey, Job, Job> = SourceOfTruth.of(
    reader = { key: JobKey ->
        require(key is JobKey.Read)
        flow {
            when (key) {
                is JobKey.Read.ById -> emit(db.getJobById(key.JobId))
            }
        }
    },
    writer = { key, input ->
        require(key is JobKey.Write)
        when (key) {
            is JobKey.Write.Create -> db.create(input)
            is JobKey.Write.ById -> db.update(key.JobId, input)
        }
    },
    delete = { key: JobKey ->
        require(key is JobKey.Clear.ById)
        db.deleteById(key.JobId)
    },
    deleteAll = db.delete()
)

fun provide(
    fetcher: Fetcher<JobKey, JobNetwork>,
    sourceOfTruth: SourceOfTruth<JobKey, Job, Job>,
    updater: Updater<JobKey, JobCommon, JobUpdaterResult>,
): MutableStore<JobKey, JobCommon> = StoreBuilder.from(
    fetcher = fetcher,
    sourceOfTruth = sourceOfTruth,
).build(updater)

interface JobApi {
    fun getJobById(jobId: Uuid): JobNetwork
    fun create(input: JobCommon): UpdaterResult
    fun update(jobId: Uuid, input: JobCommon): UpdaterResult
}

interface JobDb {
    fun getJobById(jobId: Uuid): Job
}