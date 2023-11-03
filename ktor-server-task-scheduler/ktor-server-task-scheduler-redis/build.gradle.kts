import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmTestDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm()
    sourceSets {
        commonMainDependencies {
            implementation(projects.common)
            api(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerCore)
            implementation(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerRedis.redisClient)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerCore.test)
            implementation(libs.mockk)
            implementation(libs.testcontainers.redis)
            implementation(libs.kreds)
        }
    }
}
