package io.github.flaxoos.ktor.extensions

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

fun KotlinMultiplatformExtension.nativeTarget(
    baseName: String,
    configure: KotlinNativeTargetWithHostTests.() -> Unit = {},
    hardTarget: (KotlinMultiplatformExtension.() -> KotlinNativeTargetWithHostTests)? = null
) {
    val hostOs = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
//    val nativeTarget = hardTarget?.let { this.hardTarget() } ?: when {
//        hostOs == "Mac OS X" && arch == "x86_64" -> macosX64("native")
//        hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        // Other supported targets are listed here: https://ktor.io/docs/native-server.html#targets
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }
    val nativeTarget = linuxX64("native")
    nativeTarget.apply {
        binaries {
            sharedLib {
                this.baseName = baseName
            }
        }
        configure()
    }
}