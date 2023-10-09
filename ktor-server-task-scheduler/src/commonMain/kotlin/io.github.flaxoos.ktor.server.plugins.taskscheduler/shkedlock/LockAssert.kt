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

import java.util.Deque
import java.util.LinkedList

/**
 * Asserts lock presence. The Spring ecosystem is so complicated, so one can not be sure that the lock is applied. This class
 * makes sure that the task is indeed locked.
 *
 *
 * If you use AOP with Kotlin, it does not have to work due to final methods, if you use TaskExecutor wrapper, it can be
 * broken by Sleuth,.
 */
object LockAssert {
    // using null initial value so new LinkedList is not created every time we call alreadyLockedBy
    private val activeLocksTL = ThreadLocal.withInitial<Deque<String?>?> { null }
    fun startLock(name: String?) {
        activeLocks()!!.add(name)
    }

    fun alreadyLockedBy(name: String?): Boolean {
        val activeLocks = activeLocksTL.get()
        return activeLocks != null && activeLocks.contains(name)
    }

    fun endLock() {
        val activeLocks = activeLocks()
        activeLocks!!.removeLast()
        if (activeLocks.isEmpty()) {
            activeLocksTL.remove()
        }
    }

    private fun activeLocks(): Deque<String?>? {
        if (activeLocksTL.get() == null) {
            activeLocksTL.set(LinkedList())
        }
        return activeLocksTL.get()
    }

    /**
     * Throws an exception if the lock is not present.
     */
    fun assertLocked() {
        val activeLocks = activeLocksTL.get()
        check(!(activeLocks == null || activeLocks.isEmpty())) { "The task is not locked." }
    }

    object TestHelper {
        private const val TEST_LOCK_NAME = "net.javacrumbs.shedlock.core.test-lock"

        /**
         * If pass is set to true, all LockAssert.assertLocked calls in current thread will pass.
         * To be used in unit tests only
         *
         * `
         * LockAssert.TestHelper.makeAllAssertsPass(true)
        ` *
         */
        fun makeAllAssertsPass(pass: Boolean) {
            if (pass) {
                startLock(TEST_LOCK_NAME)
            } else {
                endLock()
            }
        }
    }
}
