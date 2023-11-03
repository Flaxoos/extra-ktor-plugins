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
            implementation(libs.exposed.core)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.krontab)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduler.ktorServerTaskSchedulerCore.test)
            implementation(libs.testcontainers.postgres)
            implementation(libs.postgresql)
        }
    }
}
