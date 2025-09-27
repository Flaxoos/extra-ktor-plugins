package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaTask

private const val DOKKA = "dokka"
private const val DOKKA_JAR_TASK_NAME = "${DOKKA}Jar"

fun Project.configurePublishing() {
    registerDokkaJarTask()
    configureMavenPublications()
}

fun Project.registerDokkaJarTask() =
    tasks.register<Jar>(DOKKA_JAR_TASK_NAME) {
        val dokkaHtml = tasks.named<AbstractDokkaTask>("${DOKKA}Html")
        archiveClassifier.set("javadoc")
        from(dokkaHtml.get().outputDirectory)
    }

fun Project.configureMavenPublications() {
    val stagingDir = rootProject.layout.buildDirectory.dir("staging-deploy")

    afterEvaluate {
        the<PublishingExtension>().apply {
            repositories {
                maven {
                    name = "localStaging"
                    url = stagingDir.get().asFile.toURI()
                }
            }
            publications.withType<MavenPublication> {
                artifact(tasks.dokkaJar)
                pom {
                    name.set("${rootProject.name}: ${project.name}")
                    groupId = project.group.toString()
                    version = project.version.toString()
                    url.set("https://github.com/Flaxoos/extra-ktor-plugins")
                    inceptionYear.set("2023")
                    description.set(
                        "This project provides a suite of feature-rich, efficient, and highly customizable " +
                            "plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.",
                    )
                    scm {
                        connection.set("scm:git:https://github.com/Flaxoos/extra-ktor-plugins.git")
                        developerConnection.set("scm:git:https://github.com/Flaxoos/extra-ktor-plugins.git")
                        url.set("https://github.com/Flaxoos/extra-ktor-plugins")
                    }

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://opensource.org/license/apache-2-0")
                        }
                    }

                    developers {
                        developer {
                            id.set("flaxoos")
                            name.set("Ido Flax")
                            email.set("idoflax@gmail.com")
                        }
                    }
                }
            }
        }
    }
}

val TaskContainer.dokkaJar: TaskProvider<Jar>
    get() = named(DOKKA_JAR_TASK_NAME, Jar::class)
