package io.github.flaxoos.ktor.extensions

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.accessors.AccessorFormats.internal
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Base64

private const val JVM = "jvm"


internal fun Project.configurePublishing() {
    val dokkaHtml = tasks.named<AbstractDokkaTask>("dokkaHtml")
    val dokkaJar = tasks.register<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        from(dokkaHtml.get().outputDirectory)
    }
     fun Project.jvmShadow() {
        val mainCompilation = with(the<KotlinMultiplatformExtension>()) {
            targets.named(JVM).map { it.compilations.getByName("main") }
        }
        val sourceJar = tasks.register("sourceJar", org.gradle.api.tasks.bundling.Jar::class) {
            from(mainCompilation.map { it.allKotlinSourceSets.map { kotlinSourceSet -> kotlinSourceSet.kotlin } })
            archiveClassifier.set("sources")
        }
        val shadowJvmJar = tasks.register("shadow${JVM.capitalized()}Jar", ShadowJar::class) {
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
            mustRunAfter(tasks.named("generateMetadataFileForJvmPublication"))
            mustRunAfter(tasks.named("signJvmPublication"))
        }
        with(the<PublishingExtension>()) {
            publications {
                create("shadow${JVM.capitalized()}", MavenPublication::class) {
                    artifact(shadowJvmJar.map { it.archiveFile })
                    artifact(sourceJar)
                    artifact(dokkaJar)
                    artifactId = "${project.name}-$JVM"
                    pom.withXml {
                        (asNode().get("dependencies") as NodeList).clear()
                    }
                }
            }
        }
        tasks.withType(AbstractPublishToMaven::class) {
            if (publication?.name?.contains(JVM, ignoreCase = true) == true
                && publication?.name?.contains("shadow", ignoreCase = true) != true
            ) {
                enabled = false
            }
        }
    }
    jvmShadow()
    the<PublishingExtension>().apply {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set("${rootProject.name}: ${project.name}")
                groupId = project.group.toString()
                version = project.version.toString()
                description.set(
                    "This project provides a suite of feature-rich, efficient, and highly customizable " +
                            "plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform."
                )
                url.set("https://github.com/Flaxoos/extra-ktor-plugins")
                inceptionYear.set("2023")

                scm {
                    connection.set("scm:git:https://github.com/Flaxoos/extra-ktor-plugins.git")
                    developerConnection.set("scm:git:https://github.com/Flaxoos/extra-ktor-plugins.git")
                    url.set("https://github.com/Flaxoos/extra-ktor-plugins")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://opensource.org/license/mit/")
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
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(dokkaJar)
        artifacts {
            add("archives", tasks.named("sourcesJar"))
            add("archives", dokkaJar)
        }
    }
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
    the<SigningExtension>().apply {
        useInMemoryPgpKeys(Base64.getDecoder().decode(signingKeyArmorBase64).decodeToString(), signingPassword)
        sign(the<PublishingExtension>().publications)
    }
}



val TaskContainer.publishShadowJvmPublicationToMavenLocal: TaskProvider<PublishToMavenLocal>
    get() = named("publishShadowJvmPublicationToMavenLocal", PublishToMavenLocal::class)

