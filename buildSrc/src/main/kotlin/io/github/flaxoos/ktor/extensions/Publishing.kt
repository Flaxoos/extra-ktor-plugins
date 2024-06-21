package io.github.flaxoos.ktor.extensions

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import java.util.Base64

private const val JVM = "jvm"
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
                description.set(
                    "This project provides a suite of feature-rich, efficient, and highly customizable " +
                            "plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform."
                )
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
