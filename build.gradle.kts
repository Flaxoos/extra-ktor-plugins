import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import io.github.flaxoos.ktor.extensions.mcPassword
import io.github.flaxoos.ktor.extensions.mcUsername
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogging
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter
import ru.vyarus.gradle.plugin.python.PythonExtension

plugins {
    base
    id(
        libs.plugins.dokka
            .get()
            .pluginId,
    )
    id(
        libs.plugins.jreleaser
            .get()
            .pluginId,
    )
    id("ru.vyarus.mkdocs-build") version "3.0.0"
}

jreleaser {
    // Configure the project
    project {
        name.set("${rootProject.name}: ${project.name}")
        description.set(
            "This project provides a suite of feature-rich, efficient, and highly customizable plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.",
        )
        longDescription.set(
            "A comprehensive collection of Ktor plugins including rate limiting, Kafka integration, circuit breaker patterns, and distributed task scheduling. Built with Kotlin Multiplatform support for JVM, Native, and JS platforms.",
        )
        authors.add("Ido Flax")
        license.set("Apache-2.0")
        copyright.set("Copyright (c) 2023 Ido Flax")
        authors.add("Ido Flax")
        maintainers.add("Ido Flax")
        links {
            homepage.set("https://github.com/Flaxoos/extra-ktor-plugins")
            documentation.set("https://flaxoos.github.io/extra-ktor-plugins/")
            license.set("https://opensource.org/license/apache-2-0")
            donation.set("https://github.com/sponsors/Flaxoos")
            contact.set("Kotlin Slack - Ido Flax. idoflax@gmail.com")
        }
        inceptionYear.set("2023")
    }
    // Sign the release
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    // Deploy to Maven Central
    deploy {
        maven {
            mavenCentral {
                create("flaxoos-extra-ktor-plugins") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(
                        layout.buildDirectory
                            .dir("staging-deploy")
                            .get()
                            .asFile.absolutePath,
                    )

                    username.set(mcUsername)
                    password.set(mcPassword)

                    connectTimeout.set(20)
                    readTimeout.set(60)
                    retryDelay.set(5)
                    maxRetries.set(3)
                }
            }
        }
    }
    // Create a release in GitHub
    release {
        github {
            enabled.set(true)
            repoOwner.set("Flaxoos")
            name.set("extra-ktor-plugins")
            tagName.set("v{{projectVersion}}")
            releaseName.set("v{{projectVersion}}")
            overwrite.set(true)
            update {
                enabled.set(true)
            }
            changelog {
                enabled.set(true)
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                contributors {
                    enabled.set(false)
                }
                hide {
                    categories.add("merge")
                    contributors.add("GitHub")
                }
            }
        }
    }
}

tasks.jreleaserAssemble {
    dependsOn(
        subprojects.flatMap { sp ->
            sp.tasks.matching { it.name == "publishAllPublicationsToLocalStagingRepository" }
        },
    )
}

allprojects {
    group = "io.github.flaxoos"
    version = project.property("version") as String
}

subprojects {
    tasks.find { it.name == "build" }?.dependsOn(tasks.named("ktlintFormat"))

    tasks.withType(Test::class) {
        testLogging {
            info {
                testDetails()
            }
        }
    }

    extensions.findByType(GradleEnterpriseExtension::class)?.apply {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

mkdocs {
    sourcesDir = layout.projectDirectory.dir("documentation/mkdocs").toString()
    python.scope = PythonExtension.Scope.USER
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaCollectorTask>().configureEach {
    outputDirectory.set(file("$rootDir/documentation/mkdocs/docs/dokka"))
}

fun TestLogging.testDetails() {
    events = setOf(PASSED, SKIPPED, FAILED)
    showStandardStreams = true
    exceptionFormat = SHORT
    stackTraceFilters = setOf(TestStackTraceFilter.GROOVY)
}
