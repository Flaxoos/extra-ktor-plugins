package io.github.flaxoos.ktor

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.jacomet.gradle.plugins.logging.extension.LoggingCapabilitiesExtension
import io.github.flaxoos.kover.ColorBand.Companion.from
import io.github.flaxoos.kover.KoverBadgePluginExtension
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponentWithCoordinatesAndPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.net.URI

open class Conventions : Plugin<Project> {
    open fun KotlinMultiplatformExtension.conventionSpecifics() {}
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply("org.gradle.version-catalog")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("maven-publish")
                apply("idea")
                apply("io.kotest.multiplatform")
                apply(project.plugin("loggingCapabilities"))
                apply(project.plugin("atomicfu"))
                apply(project.plugin("kover"))
                apply(project.plugin("kover-badge"))
                apply(project.plugin("dokka"))
                apply(project.plugin("detekt"))
                apply(project.plugin("ktlint"))
                apply(project.plugin("shadow"))
            }
            group = "io.github.flaxoos"
            version = "1.0.0"

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
                }
            }

            extensions.findByType(KotlinMultiplatformExtension::class)?.apply {
                jvm {
                    jvmToolchain(versionOf("java").toInt())
                }
                val hostOs = System.getProperty("os.name")
                val arch = System.getProperty("os.arch")
                val nativeTarget = when {
                    hostOs == "Mac OS X" && arch == "x86_64" -> macosX64("native")
                    hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
                    hostOs == "Linux" -> linuxX64("native")
                    //TODO: support IOS and android for client plugins, split to two conventions for server and client
                    // Other supported targets are listed here: https://ktor.io/docs/native-server.html#targets
                    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
                }
                nativeTarget.apply {
                    binaries {
                        sharedLib {
                            baseName = "ktor"
                        }
                    }
                }
                this.sourceSets.apply {
                    commonMain {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versionOf("kotlinx-datetime")}")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versionOf("kotlinx_coroutines")}")
                            implementation("io.arrow-kt:arrow-core:${versionOf("arrow")}")
                            implementation("io.arrow-kt:arrow-fx-coroutines:${versionOf("arrow")}")
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

            tasks.withType(Test::class) {
                useJUnitPlatform()
            }

            tasks.withType<Wrapper> {
                gradleVersion = "8.2.1"
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
                        "green" from 90.0f
                    )
                )
            }

            extensions.findByType(PublishingExtension::class)?.apply {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url =
                            URI("https://maven.pkg.github.com/idoflax/${project.findProperty("github.repository.name") ?: project.name}")
                        gprWriteCredentials()
                    }
                }
            }

            extensions.findByType(LoggingCapabilitiesExtension::class)?.apply {
                enforceLogback()
            }

            extensions.findByType(LoggingCapabilitiesExtension::class)?.apply {
                enforceLogback()
            }

            tasks.register("createReleaseTag") {
                doLast {
                    createReleaseTag()
                }
            }.let { tasks.named("publish") { dependsOn(it) } }

            extensions.findByType(AtomicFUPluginExtension::class)?.apply {
                dependenciesVersion = versionOf("atomicFu")
                transformJvm = true
                jvmVariant = "FU"
            }
        }
    }
}

class KtorServerPluginConventions : Conventions() {

    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        sourceSets.apply {
            commonMain {
                with(this.project) {
                    dependencies {
                        implementation("io.ktor:ktor-server-core:${ktorVersion()}")
                        implementation("io.ktor:ktor-server-config-yaml:${ktorVersion()}")
                        implementation("io.ktor:ktor-server-auth:${ktorVersion()}")
                    }
                }
            }
            commonTest {
                with(this.project) {
                    dependencies {
                        implementation("io.ktor:ktor-server-test-host:${ktorVersion()}")
                        implementation("io.ktor:ktor-server-status-pages:${ktorVersion()}")
                    }
                }
            }
        }
    }
}

class KtorClientPluginConventions : Conventions() {
    override fun KotlinMultiplatformExtension.conventionSpecifics() {
        sourceSets.apply {
            commonMain {
                dependencies {
                    with(this.project) {
                        implementation("io.ktor:ktor-client-core:${ktorVersion()}")
                        implementation("io.ktor:ktor-client-cio:${ktorVersion()}")
                    }
                }
            }
            commonTest {
                dependencies {
                    with(this.project) {
                        implementation("io.ktor:ktor-client-mock:${ktorVersion()}")
                    }
                }
            }
            jvmTest {
                dependencies {
                    implementation("ch.qos.logback:logback-classic:1.4.9")
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

/**
 * Deletes the current tag and recreates it
 */
internal fun Project.createReleaseTag() {
    val tagName = "release/${version}"
    try {
        runCommands("git", "tag", "-d", tagName)
    } catch (e: Exception) {
        logger.warn("Failed deleting release tag. if the tag $tagName doesn't exist then this is expected", e.message)
    }
    runCommands("git", "status")
    runCommands("git", "tag", tagName)
}

/**
 * Run a command
 */
private fun runCommands(vararg commands: String): String {
    val process = ProcessBuilder(*commands).redirectErrorStream(true).start()
    process.waitFor()
    var result = ""
    process.inputStream.bufferedReader().use { it.lines().forEach { line -> result += line + "\n" } }
    val errorResult = process.exitValue() == 0
    if (!errorResult) {
        throw IllegalStateException(result)
    }
    return result
}

context(Project)
private fun MavenArtifactRepository.gprWriteCredentials() {
    credentials {
        username = gprUser
        password = gprWriteToken
    }
}

context(Project)
private fun MavenArtifactRepository.gprReadCredentials() {
    credentials {
        username = gprUser
        password = gprReadToken
    }
}

private val Project.gprWriteToken
    get() = findProperty("gpr.write.key") as String? ?: System.getenv("GPR_WRITE_TOKEN")

private val Project.gprReadToken
    get() = findProperty("gpr.read.key") as String? ?: System.getenv("GPR_READ_TOKEN")

private val Project.gprUser
    get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")

fun Project.libs() = project.the<VersionCatalogsExtension>().find("libs")

fun Project.versionOf(version: String): String =
    this.libs().get().findVersion(version).get().toString()

fun Project.library(name: String): String =
    this.libs().get().findLibrary(name).get().get().toString()

fun Project.plugin(name: String): String =
    this.libs().get().findPlugin(name).get().get().pluginId

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class KoverIgnore