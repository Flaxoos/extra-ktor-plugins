import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmMainDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    targetJvm(project)
    sourceSets {
        jvmMainDependencies {
            api(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore)
            api(libs.kotest.framework.engine)
            api(libs.kotest.framework.datatest)
            api(libs.kotest.assertions.core)
            api(libs.ktor.server.test.host)
            api(libs.kotest.extensions.testcontainers)
            api(libs.testcontainers)
            api(libs.testcontainers.postgres)
        }
    }
}
tasks.matching { it.name.contains("kover", ignoreCase = true) }.configureEach {
    enabled = false
}
tasks.withType<AbstractPublishToMaven>().configureEach {
    enabled = false
}
