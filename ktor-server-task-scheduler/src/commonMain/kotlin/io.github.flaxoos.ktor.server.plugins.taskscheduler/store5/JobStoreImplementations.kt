package io.github.flaxoos.ktor.server.plugins.taskscheduler.store5

import com.benasher44.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.OnUpdaterCompletion
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult
//
//public fun fetcher(
//    api: JobApi
//): Fetcher<JobKey, JobNetwork> = Fetcher.of { key ->
//    require(key is JobKey.Read)
//    when (key) {
//        is JobKey.Read.ById -> api.getJobById(key.jobId)
//    }
//}
//
//public fun updater(
//    api: JobApi
//): Updater<JobKey, JobCommon, JobUpdaterResult> =
//    Updater.by(
//        post = { key, input ->
//            require(key is JobKey.Write)
//            when (key) {
//                is JobKey.Write.Create -> api.create(input)
//                is JobKey.Write.ById -> api.update(key.jobId, input)
//            }
//        },
//        onCompletion = OnUpdaterCompletion(
//            onSuccess = { success: UpdaterResult.Success ->
//                UserLogger.post(StoreEvents.Update(success.status))
//            },
//            onFailure = { failure: UpdaterResult.Error ->
//                UserLogger.post(StoreEvents.Update(failure.status))
//            }
//        )
//    )
//
//public fun sourceOfTruth(
//    db: JobDb
//): SourceOfTruth<JobKey, Job, Job> = SourceOfTruth.of(
//    reader = { key: JobKey ->
//        require(key is JobKey.Read)
//        flow {
//            when (key) {
//                is JobKey.Read.ById -> emit(db.getJobById(key.jobId))
//            }
//        }
//    },
//    writer = { key, input ->
//        require(key is JobKey.Write)
//        when (key) {
//            is JobKey.Write.Create -> db.create(input)
//            is JobKey.Write.ById -> db.update(key.jobId, input)
//        }
//    },
//    delete = { key: JobKey ->
//        require(key is JobKey.Clear.ById)
//        db.deleteById(key.jobId)
//    },
//    deleteAll = db.delete()
//)
//
//@OptIn(ExperimentalStoreApi::class)
//public fun mutableStore(
//    fetcher: Fetcher<JobKey, JobNetwork>,
//    sourceOfTruth: SourceOfTruth<JobKey, Job, Job>,
//    updater: Updater<JobKey, JobCommon, JobUpdaterResult>,
//): MutableStore<JobKey, JobCommon> = StoreBuilder.from(
//    fetcher = fetcher,
//    sourceOfTruth = sourceOfTruth,
//).build(updater)
//
//public interface JobApi {
//    public fun getJobById(jobId: Uuid): JobNetwork
//    public fun create(input: JobCommon): UpdaterResult
//    public fun update(jobId: Uuid, input: JobCommon): UpdaterResult
//}
//
//public interface JobDb {
//    public fun getJobById(jobId: Uuid): Job
//    public fun create(input: JobCommon): Job
//    public fun update(jobId: Uuid, input: JobCommon): Job
//    public fun delete()
//    public fun deleteById(jobId: Uuid)
//}