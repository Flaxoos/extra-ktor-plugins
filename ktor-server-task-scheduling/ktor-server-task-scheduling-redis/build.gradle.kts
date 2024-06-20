import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.extensions.gprReadToken
import io.github.flaxoos.ktor.extensions.gprUser
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative
import io.github.flaxoos.ktor.jvmTestDependencies
import io.github.flaxoos.ktor.nativeMainDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm()
    targetNative()
    sourceSets {
        commonMainDependencies {
            api(projects.common)
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
//            implementation(libs.redis.mp.client)
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingRedis.redisClient)
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
