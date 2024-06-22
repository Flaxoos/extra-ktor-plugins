package io.github.flaxoos.ktor

import dev.jacomet.gradle.plugins.logging.extension.LoggingCapabilitiesExtension
import io.github.flaxoos.kover.ColorBand.Companion.from
import io.github.flaxoos.kover.KoverBadgePluginExtension
import io.github.flaxoos.ktor.extensions.configurePublishing
import io.github.flaxoos.ktor.extensions.enableContextReceivers
import io.github.flaxoos.ktor.extensions.gprReadCredentials
import io.github.flaxoos.ktor.extensions.library
import io.github.flaxoos.ktor.extensions.plugin
import io.github.flaxoos.ktor.extensions.setLanguageAndApiVersions
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.versionOf
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

open class Conventions : Plugin<Project> {
    open fun KotlinMultiplatformExtension.conventionSpecifics() {}
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("maven-publish")
                apply("signing")
                apply("idea")
                apply("io.kotest.multiplatform")
                apply(project.plugin("loggingCapabilities"))
                apply(project.plugin("atomicfu"))
                apply(project.plugin("kover"))
                apply(project.plugin("kover-badge"))
                apply(project.plugin("dokka"))
                apply(project.plugin("detekt"))
                apply(project.plugin("ktlint"))
            }
            repositories {
                mavenCentral()
                maven {
                    url = uri("https://maven.pkg.github.com/flaxoos/flax-gradle-plugins")
                    gprReadCredentials()
                }
            }
            enableContextReceivers()

            extensions.findByType(KotlinMultiplatformExtension::class)?.apply {
                targetJvm()
                this.sourceSets.apply {
                    commonMainDependencies {
                        implementation(library("kotlinx-datetime"))
                        implementation(library("kotlinx-coroutines-core"))
                        implementation(library("arrow-core"))
                        implementation(library("arrow-fx-coroutines"))
                        implementation(library("kotlin-logging"))

                    }

                    commonTestDependencies {
                        implementation(kotlin("test"))
                        implementation(library("kotlinx-coroutines-test"))
                        implementation(library("kotest-framework-engine"))
                        implementation(library("kotest-framework-datatest"))
                        implementation(library("kotest-assertions-core"))
                    }

                    jvmMainDependencies {
                        implementation(library("logback-classic"))
                    }

                    jvmTestDependencies {
                        implementation(library("kotest-runner-junit5"))
                        implementation(library("mockk"))
                        implementation(library("mockk-agent-jvm"))
                    }
                }
                this.conventionSpecifics()
            }

            setLanguageAndApiVersions()

            the<IdeaModel>().apply {
                module {
                    this.isDownloadSources = true
                    this.isDownloadJavadoc = true
                }
            }

            the<DetektExtension>().apply {
                config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
                buildUponDefaultConfig = true
            }

            tasks.named("build").configure {
                dependsOn("ktlintFormat")
                dependsOn(tasks.matching { it.name.matches(Regex("detekt(?!.*Baseline).*\\b(Main|Test)\\b\n")) })
            }

            tasks.withType(Test::class) {
                useJUnitPlatform()
            }

            tasks.withType<Wrapper> {
                gradleVersion = "8.3"
                distributionType = Wrapper.DistributionType.BIN
            }
            extensions.findByType(KoverReportExtension::class)?.apply {
                defaults {
                    html { onCheck = true }
                    verify {
                        rule {
                            isEnabled = true
                            minBound(60)
                        }
                        onCheck = true
                    }
                }
            }

            extensions.findByType<KoverBadgePluginExtension>()?.apply {
                readme.set(project.file("README.md"))
                spectrum.set(
                    listOf(
                        "red" from 0.0f,
                        "yellow" from 50.0f,
                        "green" from 90.0f,
                    ),
                )
            }

            the<LoggingCapabilitiesExtension>().apply {
                enforceLogback()
            }

            extensions.findByType(AtomicFUPluginExtension::class)?.apply {
                dependenciesVersion = versionOf("atomicFu")
                transformJvm = true
                jvmVariant = "FU"
            }

            if (hasProperty("buildScan")) {
                extensions.findByName("buildScan")?.withGroovyBuilder {
                    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
                    setProperty("termsOfServiceAgree", "yes")
                }
            }

            configurePublishing()
        }
    }


}

class KtorServerPluginConventions : Conventions() {

    @OptIn(ExternalVariantApi::class)
    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        sourceSets.apply {
            commonMainDependencies {
                implementation(project.library("ktor-server-core"))
                implementation(project.library("ktor-server-config-yaml"))
                implementation(project.library("ktor-server-auth"))
            }
            commonTestDependencies {
                implementation(project.library("ktor-server-test-host"))
                implementation(project.library("ktor-server-status-pages"))
            }
        }
    }
}

class KtorClientPluginConventions : Conventions() {

    @OptIn(ExternalVariantApi::class)
    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        with(this.project) {
            sourceSets.apply {
                commonMainDependencies {
                    implementation(library("ktor-client-core"))
                }
                commonTestDependencies {
                    implementation(library("ktor-client-mock"))
                }
            }
        }
    }
}

fun NamedDomainObjectCollection<KotlinSourceSet>.commonMainDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    get("commonMain").apply {
        dependencies {
            configure()
        }
    }

fun NamedDomainObjectCollection<KotlinSourceSet>.commonTestDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    get("commonTest").apply {
        dependencies {
            configure()
        }
    }

fun NamedDomainObjectCollection<KotlinSourceSet>.jvmMainDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    findByName("jvmMain")?.apply {
        dependencies {
            configure()
        }
    }

fun NamedDomainObjectCollection<KotlinSourceSet>.jvmTestDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    findByName("jvmTest")?.apply {
        dependencies {
            configure()
        }
    }

fun NamedDomainObjectCollection<KotlinSourceSet>.nativeMainDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    findByName("nativeMain")?.apply {
        dependencies {
            configure()
        }
    }

fun NamedDomainObjectCollection<KotlinSourceSet>.nativeTestDependencies(configure: KotlinDependencyHandler.() -> Unit) =
    findByName("nativeTest")?.apply {
        dependencies {
            configure()
        }
    }

private fun Project.ktorVersion() = versionOf("ktor")


fun Project.projectDependencies(configuration: DependencyHandlerScope.() -> Unit) =
    DependencyHandlerScope.of(dependencies).configuration()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class KoverIgnore
