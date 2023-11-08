import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    kotlin("multiplatform")
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
