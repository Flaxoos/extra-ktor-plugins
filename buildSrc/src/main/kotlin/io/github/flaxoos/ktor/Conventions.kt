package io.github.flaxoos.ktor

import dev.jacomet.gradle.plugins.logging.extension.LoggingCapabilitiesExtension
import io.github.flaxoos.kover.ColorBand.Companion.from
import io.github.flaxoos.kover.KoverBadgePluginExtension
import io.github.flaxoos.ktor.extensions.configurePublishing
import io.github.flaxoos.ktor.extensions.gprReadCredentials
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

open class Conventions : Plugin<Project> {
    open fun KotlinMultiplatformExtension.conventionSpecifics() {}
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply("org.gradle.java-library")
                apply("java-library-distribution")
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
            tasks.withType(KotlinCompilationTask::class) {
                compilerOptions {
                    freeCompilerArgs.add("-Xcontext-receivers")
                    languageVersion.set(KOTLIN_1_9)
                    apiVersion.set(KOTLIN_1_9)
                }
            }

            extensions.findByType(KotlinMultiplatformExtension::class)?.apply {
                explicitApi()
                jvm {
                    jvmToolchain(versionOf("java").toInt())
                    tasks.named("jvmJar", Jar::class).configure {
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        from(
                            listOf(
                                configurations["jvmCompileClasspath"],
                                configurations["jvmRuntimeClasspath"],
                            ).map { it.map { if (it.isDirectory) it else zipTree(it) } },
                        )
                    }
                }

                this.sourceSets.apply {
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versionOf("kotlinx-datetime")}")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versionOf("kotlinx_coroutines")}")
                            implementation("io.arrow-kt:arrow-core:${versionOf("arrow")}")
                            implementation("io.arrow-kt:arrow-fx-coroutines:${versionOf("arrow")}")
                            implementation(library("kotlin-logging"))
                        }
                    }

                    commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${versionOf("kotlinx_coroutines")}")
                            implementation("io.kotest:kotest-framework-engine:${versionOf("kotest")}")
                            implementation("io.kotest:kotest-framework-datatest:${versionOf("kotest")}")
                            implementation("io.kotest:kotest-assertions-core:${versionOf("kotest")}")
                        }
                    }

                    jvmTest {
                        dependencies {
                            implementation("io.kotest:kotest-runner-junit5:${versionOf("kotest")}")
                            implementation("ch.qos.logback:logback-classic:${project.versionOf("logback")}")
                            implementation(library("mockk"))
                        }
                    }
                }
                this.conventionSpecifics()
            }

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

            extensions.findByType(LoggingCapabilitiesExtension::class)?.apply {
                enforceLogback()
            }

            extensions.findByType(LoggingCapabilitiesExtension::class)?.apply {
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

            afterEvaluate {
                configurePublishing()
            }
        }
    }
}

class KtorServerPluginConventions : Conventions() {

    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        // Support mac OS?
        // macosArm64()
        // macosX64()
        linuxX64("native") {
            binaries {
                sharedLib {
                    baseName = "ktor"
                }
            }
        }
        sourceSets.apply {
            commonMain {
                with(this.project) {
                    dependencies {
                        implementation(library("ktor-server-core"))
                        implementation(library("ktor-server-config-yaml"))
                        implementation(library("ktor-server-auth"))
                    }
                }
            }
            commonTest {
                with(this.project) {
                    dependencies {
                        implementation(library("ktor-server-test-host"))
                        implementation(library("ktor-server-status-pages"))
                    }
                }
            }
        }
    }
}

class KtorClientPluginConventions : Conventions() {
    @OptIn(ExternalVariantApi::class)
    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        with(this.project) {
//            js()
            ios()
            sourceSets.apply {
                commonMain {
                    dependencies {
                        implementation(library("ktor-client-core"))
                    }
                }
                commonTest {
                    dependencies {
                        implementation(library("ktor-client-mock"))
                    }
                }

                jvmTest {
                    dependencies {
                        implementation(library("logback-classic"))
                    }
                }
            }
        }
    }
}

private fun NamedDomainObjectCollection<KotlinSourceSet>.commonMain(configure: KotlinSourceSet.() -> Unit) =
    get("commonMain").apply { configure() }

private fun NamedDomainObjectCollection<KotlinSourceSet>.commonTest(configure: KotlinSourceSet.() -> Unit) =
    get("commonTest").apply { configure() }

private fun NamedDomainObjectCollection<KotlinSourceSet>.jvmMain(configure: KotlinSourceSet.() -> Unit) =
    get("jvmMain").apply { configure() }

private fun NamedDomainObjectCollection<KotlinSourceSet>.jvmTest(configure: KotlinSourceSet.() -> Unit) =
    get("jvmTest").apply { configure() }

private fun Project.ktorVersion() = versionOf("ktor")

fun Project.libs() = project.the<VersionCatalogsExtension>().find("libs")

fun Project.versionOf(version: String): String =
    this.libs().get().findVersion(version).get().toString()

fun Project.library(name: String): String =
    this.libs().get().findLibrary(name).get().get().toString()

fun Project.plugin(name: String): String =
    this.libs().get().findPlugin(name).get().get().pluginId

fun Project.projectDependencies(configuration: DependencyHandlerScope.() -> Unit) =
    DependencyHandlerScope.of(dependencies).configuration()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class KoverIgnore
