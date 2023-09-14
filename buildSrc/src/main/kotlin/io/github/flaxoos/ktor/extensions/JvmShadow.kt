package io.github.flaxoos.ktor.extensions

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.internal.impldep.com.amazonaws.util.XpathUtils.asNode
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val JVM = "jvm"

fun Project.jvmShadow() {
    val mainCompilation = with(the<KotlinMultiplatformExtension>()) {
        targets.named(JVM).map { it.compilations.getByName("main") }
    }
    val shadowJvmJar = tasks.create("shadow${JVM.capitalized()}Jar", ShadowJar::class).apply {
        archiveBaseName = "${project.name}-$JVM"
        from(mainCompilation.map { it.output })
        archiveClassifier.set("")
        configurations = listOf(
            project.configurations["${JVM}CompileClasspath"],
            project.configurations["${JVM}RuntimeClasspath"],
        )
        dependsOn(tasks.named("jvmJar"))
        dependencies {
            exclude(dependency("org.slf4j:slf4j-api"))
        }
        this.configurations
    }
    with(the<PublishingExtension>()) {
        publications {
            create("shadow${JVM.capitalized()}", MavenPublication::class) {
                artifact(shadowJvmJar.archiveFile)
                artifactId = "${project.name}-$JVM"
                pom.withXml {
                    (asNode().get("dependencies") as NodeList).clear()
                }
            }
        }
    }
}

val TaskContainer.publishShadowJvmPublicationToMavenLocal: TaskProvider<PublishToMavenLocal>
    get() = named("publishShadowJvmPublicationToMavenLocal", PublishToMavenLocal::class)
