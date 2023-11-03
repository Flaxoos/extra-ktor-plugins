import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    kotlin("multiplatform")
}

kotlin {
    targetJvm()
    targetNative("common")
    macosArm64("native-macos") {
        binaries {
            sharedLib {
                this.baseName = "common"
            }
        }
    }
}
