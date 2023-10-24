package io.github.flaxoos.ktor.server.plugins.taskscheduler.managers


import io.flaxoos.github.knedis.RedisConnectionPool
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

public interface LockManager<LOCK_KEY> {
    public suspend fun acquireLock(key: LOCK_KEY): Boolean
    public suspend fun isLocked(key: LOCK_KEY)
    public suspend fun releaseLock(key: LOCK_KEY)
}

//class JobLockStoreManager<Network : Any, Local : Any, Converter, Validator, Updater>(
//    fetch: suspend (key: Uuid) -> Network
//) {
//    init {
//        StoreBuilder
//            .from(
//                fetcher = Fetcher.of {
//                    fetch
//                },
//                sourceOfTruth = SourceOfTruth.of(
//                    reader = {},
//                    writer = {},
//                    delete = {},
//                    deleteAll = {}
//    }
//}


