import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmMainDependencies
import io.github.flaxoos.ktor.jvmTestDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm(project)
    sourceSets {
        jvmMainDependencies {
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
            api(libs.mongodb.driver.kotlin.coroutine)
            api(libs.mongodb.bson.kotlinx)
            api(libs.reactor.core)
            implementation(libs.kotlinx.coroutines.reactive)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.test)
            implementation(libs.testcontainers.mongodb)
        }
    }
}
