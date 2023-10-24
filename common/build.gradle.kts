import io.github.flaxoos.ktor.extensions.nativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    nativeTarget("common")
    macosArm64("native-macos")
}
