import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    id("ktor-server-plugin-conventions")
    kotlin("plugin.serialization") version libs.versions.kotlin.asProvider().get()
}

kotlin {
    explicitApi()
    targetNative()
    sourceSets {
        commonMainDependencies {
            api(libs.krontab)
        }
    }
}
