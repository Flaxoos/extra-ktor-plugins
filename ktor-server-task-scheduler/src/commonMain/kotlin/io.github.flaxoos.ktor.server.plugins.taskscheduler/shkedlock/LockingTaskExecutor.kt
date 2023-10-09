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


interface LockingTaskExecutor {
    /**
     * Executes task if it's not already running.
     */
    fun executeWithLock(task: Job, lockConfig: LockConfiguration): Boolean

    @Throws(Throwable::class)
    fun executeWithLock(task: Task, lockConfig: LockConfiguration)

    /**
     * Executes task.
     */
    fun <T> executeWithLock(task: TaskWithResult<T>, lockConfig: LockConfiguration): TaskResult<T?> {
        throw UnsupportedOperationException()
    }

    fun interface Task {
        fun call()
    }

    fun interface TaskWithResult<T> {
        fun call(): T
    }

    class TaskResult<T> private constructor(
        private val executed: Boolean,
        val result: T?
    ) {

        fun wasExecuted(): Boolean {
            return executed
        }

        companion object {
            fun <T> result(result: T?): TaskResult<T> {
                return TaskResult(true, result)
            }

            fun <T> notExecuted(): TaskResult<T?> {
                return TaskResult(false, null)
            }
        }
    }
}
