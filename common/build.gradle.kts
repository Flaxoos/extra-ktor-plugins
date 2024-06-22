import io.github.flaxoos.ktor.extensions.configureMavenPublications
import io.github.flaxoos.ktor.extensions.configurePublishing
import io.github.flaxoos.ktor.extensions.configureSigning
import io.github.flaxoos.ktor.extensions.registerDokkaJarTask
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("signing")
    id(libs.plugins.dokka.get().pluginId)
}

kotlin {
    targetJvm()
    targetNative()
    macosArm64("native-macos") {
        binaries {
            staticLib()
        }
    }
}
configurePublishing()
