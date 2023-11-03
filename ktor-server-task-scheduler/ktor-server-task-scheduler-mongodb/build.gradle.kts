import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmMainDependencies
import io.github.flaxoos.ktor.jvmTestDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm()
    sourceSets {
        jvmMainDependencies {
            api(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerCore)
            implementation(libs.mongodb.driver.kotlin.coroutine)
            implementation(libs.mongodb.bson.kotlinx)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerCore.test)
            implementation(libs.testcontainers.mongodb)
        }
    }
}
