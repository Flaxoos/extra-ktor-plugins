/**
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example

import io.github.flaxoos.ktor.server.plugins.taskscheduler.shkedlock.ClockProvider
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory
import java.util.Objects

/**
 * Default [LockingTaskExecutor] implementation.
 */
class DefaultLockingTaskExecutor(lockProvider: LockProvider) : LockingTaskExecutor {
    private val lockProvider: LockProvider

    init {
        this.lockProvider = Objects.requireNonNull(lockProvider)
    }

    override fun executeWithLock(task: Job, lockConfig: LockConfiguration) {
        try {
            executeWithLock(LockingTaskExecutor.Task { task.run() }, lockConfig)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Error) {
            throw e
        } catch (throwable: Throwable) {
            // Should not happen
            throw IllegalStateException(throwable)
        }
    }

    @Throws(Throwable::class)
    override fun executeWithLock(task: LockingTaskExecutor.Task, lockConfig: LockConfiguration) {
        executeWithLock<Any>({
            task.call()
            null
        }, lockConfig)
    }

    @Throws(Throwable::class)
    override fun <T> executeWithLock(
        task: LockingTaskExecutor.TaskWithResult<T>,
        lockConfig: LockConfiguration
    ): LockingTaskExecutor.TaskResult<T?> {
        val lockName = lockConfig.name
        if (net.javacrumbs.shedlock.core.LockAssert.alreadyLockedBy(lockName)) {
            logger.debug("Already locked '{}'", lockName)
            return LockingTaskExecutor.TaskResult.Companion.result<T?>(task.call())
        }
        val lock = lockProvider.lock(lockConfig)
        return if (lock!!.isPresent) {
            try {
                LockAssert.startLock(lockName)
                LockExtender.startLock(lock.get())
                logger.debug(
                    "Locked '{}', lock will be held at most until {}",
                    lockName,
                    lockConfig.lockAtMostUntil
                )
                LockingTaskExecutor.TaskResult.Companion.result<T?>(task.call())
            } finally {
                LockAssert.endLock()
                val activeLock = LockExtender.endLock()
                if (activeLock != null) {
                    activeLock.unlock()
                } else {
                    // This should never happen, but I do not know any better way to handle the null case.
                    logger.warn("No active lock, please report this as a bug.")
                    lock.get().unlock()
                }
                if (logger.isDebugEnabled) {
                    val lockAtLeastUntil = lockConfig.lockAtLeastUntil
                    val now = ClockProvider.now()
                    if (lockAtLeastUntil.isAfter(now)) {
                        logger.debug(
                            "Task finished, lock '{}' will be released at {}",
                            lockName,
                            lockAtLeastUntil
                        )
                    } else {
                        logger.debug(
                            "Task finished, lock '{}' released",
                            lockName
                        )
                    }
                }
            }
        } else {
            logger.debug("Not executing '{}'. It's locked.", lockName)
            LockingTaskExecutor.TaskResult.Companion.notExecuted<T?>()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultLockingTaskExecutor::class.java)
    }
}
