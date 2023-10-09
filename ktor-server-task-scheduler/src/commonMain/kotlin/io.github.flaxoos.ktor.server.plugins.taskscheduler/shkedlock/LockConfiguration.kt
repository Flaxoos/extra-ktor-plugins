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
import java.time.Instant
import java.util.Objects
import kotlin.time.Duration

/**
 * Lock configuration.
 */
class LockConfiguration(createdAt: Instant, name: String, lockAtMostFor: Duration, lockAtLeastFor: Duration) {
    private val createdAt: Instant
    val name: String

    /**
     * The lock is held until this duration passes, after that it's automatically released (the process holding it has most likely
     * died without releasing the lock) Can be ignored by providers which can detect dead processes (like Zookeeper)
     */
    val lockAtMostFor: Duration

    /**
     * The lock will be held at least this duration even if the task holding the lock finishes earlier.
     */
    val lockAtLeastFor: Duration

    /**
     * Creates LockConfiguration. There are two types of lock providers. One that uses "db time" which requires relative
     * values of lockAtMostFor and lockAtLeastFor (currently it's only JdbcTemplateLockProvider). Second type of
     * lock provider uses absolute time calculated from `createdAt`.
     *
     * @param createdAt
     * @param name
     * @param lockAtMostFor
     * @param lockAtLeastFor
     */
    init {
        this.createdAt = Objects.requireNonNull(createdAt)
        this.name = Objects.requireNonNull(name)
        this.lockAtMostFor = Objects.requireNonNull(lockAtMostFor)
        this.lockAtLeastFor = Objects.requireNonNull(lockAtLeastFor)
        require(lockAtLeastFor.compareTo(lockAtMostFor) <= 0) { "lockAtLeastFor is longer than lockAtMostFor for lock '$name'." }
        require(!lockAtMostFor.isNegative) { "lockAtMostFor is negative '$name'." }
        require(!name.isEmpty()) { "lock name can not be empty" }
    }

    val lockAtMostUntil: Instant
        get() = createdAt.plus(lockAtMostFor)
    val lockAtLeastUntil: Instant
        get() = createdAt.plus(lockAtLeastFor)
    val unlockTime: Instant
        /**
         * Returns either now or lockAtLeastUntil whichever is later.
         */
        get() {
            val now = ClockProvider.now()
            val lockAtLeastUntil = lockAtLeastUntil
            return if (lockAtLeastUntil.isAfter(now)) lockAtLeastUntil else now
        }

    override fun toString(): String {
        return "LockConfiguration{" +
                "name='" + name + '\'' +
                ", lockAtMostFor=" + lockAtMostFor +
                ", lockAtLeastFor=" + lockAtLeastFor +
                '}'
    }
}
