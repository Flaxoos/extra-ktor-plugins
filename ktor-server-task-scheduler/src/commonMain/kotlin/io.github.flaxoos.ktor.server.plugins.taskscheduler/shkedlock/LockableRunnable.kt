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

/**
 * Executes wrapped Job using [LockManager.executeWithLock]
 */
class LockableJob(
    private val task: Job,
    private val lockManager: LockManager
) : Job {

    override fun start(): Boolean =
        lockManager.executeWithLock(task)

}
