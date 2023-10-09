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

import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

/**
 * Default implementation [LockManager] implementation.
 */
class DefaultLockManager(
    lockingTaskExecutor: LockingTaskExecutor,
    lockConfigurationExtractor: LockConfigurationExtractor
) : LockManager {
    private val lockingTaskExecutor: LockingTaskExecutor
    private val lockConfigurationExtractor: LockConfigurationExtractor

    constructor(lockProvider: LockProvider, lockConfigurationExtractor: LockConfigurationExtractor) : this(
        DefaultLockingTaskExecutor(lockProvider), lockConfigurationExtractor
    )

    init {
        this.lockingTaskExecutor = lockingTaskExecutor
        this.lockConfigurationExtractor = lockConfigurationExtractor
    }

    override fun executeWithLock(task: Job) {
        val lockConfiguration = lockConfigurationExtractor.getLockConfiguration(task)
        lockConfiguration?.let {
            lockingTaskExecutor.executeWithLock(task, it)
        } ?: {
            logger.debug("No lock configuration for {}. Executing without lock.", task)
            task.start()
        }
    }
}
