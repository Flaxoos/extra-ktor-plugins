import io.github.flaxoos.ktor.extensions.shadowJvmJar
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmMainDependencies
import io.github.flaxoos.ktor.jvmTestDependencies
import kotlinx.atomicfu.plugin.gradle.AtomicFUTransformTask
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    explicitApi()
    targetJvm()
    sourceSets {
        jvmMainDependencies {
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
            implementation(libs.exposed.core)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.krontab)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.test)
            implementation(libs.testcontainers.postgres)
            implementation(libs.postgresql)
        }
    }
}

tasks.withType(DokkaTask::class).configureEach {
    dependsOn(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.dependencyProject.tasks.shadowJvmJar)
}

tasks.withType(AtomicFUTransformTask::class).configureEach {
    dependsOn(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.dependencyProject.tasks.shadowJvmJar)
}
