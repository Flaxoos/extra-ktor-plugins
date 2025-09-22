package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

private const val KOTLIN = "kotlin"

fun Project.enableContextReceivers() {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
}

fun Project.setLanguageAndApiVersions() {
    tasks.withType(KotlinCompilationTask::class) {
        compilerOptions {
            val gradleKotlinVersion =
                KotlinVersion.valueOf(
                    KOTLIN.uppercase() + "_" + versionOf(KOTLIN).substringBeforeLast(".").replace(".", "_"),
                )
            languageVersion.set(gradleKotlinVersion)
            apiVersion.set(gradleKotlinVersion)
        }
    }
}
