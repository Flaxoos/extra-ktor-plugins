package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType


fun Project.jitpackArtifacts() {
    tasks.register("copyArtifactsForJitpack", Copy::class) {
        from(tasks.withType(Jar::class))
        into(rootProject.layout.buildDirectory.dir("jitpack"))
    }
}

val TaskContainer.copyArtifactsForJitpack: TaskProvider<Copy>
    get() = named("copyArtifactsForJitpack", Copy::class)