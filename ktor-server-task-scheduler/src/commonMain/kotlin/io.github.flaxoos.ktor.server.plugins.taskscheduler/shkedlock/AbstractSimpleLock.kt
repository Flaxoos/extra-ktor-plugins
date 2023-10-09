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
import java.util.Optional
import kotlin.time.Duration

abstract class AbstractSimpleLock protected constructor(protected val lockConfiguration: LockConfiguration) :
    SimpleLock {
    private var valid = true
    override fun unlock() {
        checkValidity()
        doUnlock()
        valid = false
    }

    protected abstract fun doUnlock()
    override fun extend(lockAtMostFor: Duration, lockAtLeastFor: Duration): Optional<SimpleLock> {
        checkValidity()
        val result =
            doExtend(LockConfiguration(ClockProvider.now(), lockConfiguration.name, lockAtMostFor, lockAtLeastFor))
        valid = false
        return result
    }

    protected fun doExtend(newConfiguration: LockConfiguration?): Optional<SimpleLock> {
        throw UnsupportedOperationException()
    }

    private fun checkValidity() {
        check(valid) { "Lock " + lockConfiguration.name + " is not valid, it has already been unlocked or extended" }
    }
}
