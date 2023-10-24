import io.github.flaxoos.ktor.extensions.nativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    jvm()
    nativeTarget("common")
    macosArm64("native-macos")
}
