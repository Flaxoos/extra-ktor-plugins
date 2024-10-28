package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

fun KotlinMultiplatformExtension.targetNative(configure: KotlinNativeTargetWithHostTests.() -> Unit = {}) {
    val nativeTarget = linuxX64("native")
    nativeTarget.apply {
        binaries.sharedLib()
        configure()
    }
}

fun KotlinMultiplatformExtension.targetJvm(project: Project) {
    jvmToolchain(project.versionOf("java").toInt())
    jvm()
}
