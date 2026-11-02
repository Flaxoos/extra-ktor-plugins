import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.extensions.targetNative
import io.github.flaxoos.ktor.jvmTestDependencies

plugins {
    id("ktor-server-plugin-conventions")
    kotlin("plugin.serialization") version
        libs.versions.kotlin
            .asProvider()
            .get()
}

kotlin {
    explicitApi()
    targetNative()
    sourceSets {
        commonMainDependencies {
            api(libs.krontab)
        }
        jvmTestDependencies {
            implementation(projects.ktorServerTaskScheduling.ktorServerTaskSchedulingCore.test)
        }
    }
}

koverReport {
    defaults {
        verify {
            onCheck = false
        }
    }
}
