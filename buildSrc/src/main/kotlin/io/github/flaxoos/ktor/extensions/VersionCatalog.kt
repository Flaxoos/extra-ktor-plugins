package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

fun Project.libs() = project.the<VersionCatalogsExtension>().find("libs")

fun Project.versionOf(version: String): String =
    this.libs().get().findVersion(version).get().toString()

fun Project.library(name: String): String =
    this.libs().get().findLibrary(name).get().get().toString()

fun Project.bundle(name: String): String =
    this.libs().get().findBundle(name).get().get().toString()

fun Project.plugin(name: String): String =
    this.libs().get().findPlugin(name).get().get().pluginId