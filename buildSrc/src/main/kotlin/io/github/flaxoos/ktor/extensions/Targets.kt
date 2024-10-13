package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
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
    jvm {
        project.tasks.named("jvmJar", Jar::class).configure {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(
                listOf(
                    project.configurations["jvmCompileClasspath"],
                    project.configurations["jvmRuntimeClasspath"],
                ).map { config ->
                    config.map {
                        if (it.isDirectory) {
                            it
                        } else {
                            project.zipTree(it)
                        }
                    }
                },
            )
        }
    }
}
