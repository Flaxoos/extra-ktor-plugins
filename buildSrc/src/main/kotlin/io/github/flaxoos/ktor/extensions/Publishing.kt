package io.github.flaxoos.ktor.extensions

import com.github.jengelman.gradle.plugins.shadow.internal.DependencyFilter
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.NodeList
import kotlinx.atomicfu.plugin.gradle.AtomicFUTransformTask
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Base64

private const val JVM = "jvm"
private fun shadowJvmJarTaskName() = "shadow${JVM.capitalized()}Jar"
private const val DOKKA_JAR = "dokkaJar"
private const val DOKKA_HTML = "dokkaHtml"

fun Project.configurePublishing() {
    registerDokkaJarTask()
    configureSigning()
    configureMavenPublications()

}

fun Project.registerDokkaJarTask() =
    tasks.register<Jar>(DOKKA_JAR) {
        archiveClassifier.set("javadoc")
        from(tasks.named<AbstractDokkaTask>(DOKKA_HTML).get().outputDirectory)
    }

fun Project.configureSigning() {
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
    the<SigningExtension>().apply {
        useInMemoryPgpKeys(Base64.getDecoder().decode(signingKeyArmorBase64).decodeToString(), signingPassword)
        sign(the<PublishingExtension>().publications)
    }
}

fun Project.configureMavenPublications() {
    the<PublishingExtension>().apply {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set("${rootProject.name}: ${project.name}")
                groupId = project.group.toString()
                version = project.version.toString()
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
        tasks.find { it.name == DOKKA_JAR }?.let { dokkaJar ->
            dependsOn(dokkaJar)
            artifacts {
                add("archives", tasks.named("sourcesJar"))
                add("archives", dokkaJar)
            }
        }
    }
}

/**
 * Creates a shadow JAR task for the JVM target.
 */
fun Project.createJvmShadowJar(dependencyFilter: DependencyFilter.() -> Unit = {}) {
    val mainCompilation = with(the<KotlinMultiplatformExtension>()) {
        targets.findByName(JVM)?.compilations?.getByName("main")
    }?.let { provider { it } } ?: return
    val sourceJar = tasks.register("sourceJar", org.gradle.api.tasks.bundling.Jar::class) {
        from(mainCompilation.map { it.allKotlinSourceSets.map { kotlinSourceSet -> kotlinSourceSet.kotlin } })
        archiveClassifier.set("sources")
    }
    val shadowJvmJar = tasks.register(shadowJvmJarTaskName(), ShadowJar::class) {
        archiveBaseName = "${project.name}-$JVM"
        from(mainCompilation.map { it.output })
        archiveClassifier.set("")
        configurations = listOf(
            project.configurations["${JVM}CompileClasspath"],
            project.configurations["${JVM}RuntimeClasspath"],
        )
        dependsOn(tasks.named("jvmJar"))
        dependencies {
            dependencyFilter()
        }
        mustRunAfter(tasks.named("generateMetadataFileForJvmPublication"))
        mustRunAfter(tasks.named("signJvmPublication"))
    }
    tasks.withType<DokkaTaskPartial>().configureEach {
        dependsOn(shadowJvmJar)
    }
    with(the<PublishingExtension>()) {
        publications {
            create("shadow${JVM.capitalized()}", MavenPublication::class) {
                artifact(shadowJvmJar.map { it.archiveFile })
                artifact(sourceJar)
                artifact(tasks.named(DOKKA_JAR))
                artifactId = "${project.name}-$JVM"
                pom.withXml {
                    asNode().appendNode("repositories").apply {
                        appendNode("repository").apply {
                            appendNode("maven") {
                                appendNode("url", "https://packages.confluent.io/maven/")
                                appendNode("url", "https://packages.confluent.io/maven/")
                            }
                        }
                    }
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
    tasks.withType(DokkaTask::class).configureEach {
        dependsOn(shadowJvmJar)
    }
    tasks.withType(AtomicFUTransformTask::class).configureEach {
        dependsOn(shadowJvmJar)
    }
}

val TaskContainer.shadowJvmJar: TaskProvider<Jar>
    get() = named(shadowJvmJarTaskName(), Jar::class)

val TaskContainer.publishShadowJvmPublicationToMavenLocal: TaskProvider<PublishToMavenLocal>
    get() = named("publishShadowJvmPublicationToMavenLocal", PublishToMavenLocal::class)

