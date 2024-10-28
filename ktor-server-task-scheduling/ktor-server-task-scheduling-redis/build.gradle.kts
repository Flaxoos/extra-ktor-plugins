import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative
import io.github.flaxoos.ktor.jvmTestDependencies
import io.github.flaxoos.ktor.nativeMainDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm(project)
    targetNative()
    sourceSets {
        commonMainDependencies {
            api(projects.common)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
            implementation(libs.redis.mp.client)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.test)
            implementation(libs.mockk)
            implementation(libs.testcontainers.redis)
            implementation(libs.kreds)
        }
        nativeMainDependencies {
            api(projects.common)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
        }
    }
}
