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
            api(libs.exposed.core)
            api(libs.exposed.jdbc)
            api(libs.exposed.dao)
            api(libs.exposed.kotlin.datetime)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.test)
            implementation(libs.testcontainers.postgres)
            implementation(libs.postgresql)
        }
    }
}
