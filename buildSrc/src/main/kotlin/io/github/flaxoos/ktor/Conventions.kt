package io.flax.ktor

import dev.jacomet.gradle.plugins.logging.extension.LoggingCapabilitiesExtension
import io.flax.kover.ColorBand.Companion.from
import io.flax.kover.KoverBadgePluginExtension
import io.flax.kover.Names.KOVER_BADGE_TASK_NAME
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.net.URI

fun Project.libs() = project.the<VersionCatalogsExtension>()

fun Project.versionOf(version: String): String =
    this.libs().find("libs").get().findVersion(version).get().toString()


open class Conventions : Plugin<Project> {
    open fun KotlinMultiplatformExtension.conventionSpecifics() {}
    override fun apply(project: Project) {
        with(project) {
            with(plugins) {
                apply("org.gradle.version-catalog")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("maven-publish")
                apply("io.kotest.multiplatform")
                apply("org.jetbrains.kotlinx.kover")
                apply("dev.jacomet.logging-capabilities")
                apply("kotlinx-atomicfu")
                apply("org.jlleitschuh.gradle.ktlint")
                apply("io.flax.kover-badge")
            }
            group = "io.flax"
            version = "1.0.0"

            repositories {
                mavenCentral()
                maven {
                    url = uri("https://maven.pkg.github.com/idoflax/flax-gradle-plugins")
                    gprCredentials()
                }
            }
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class) {
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
                            minBound(90)
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
                        gprCredentials()
                    }
                }
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

    context(Project)
    private fun MavenArtifactRepository.gprCredentials() {
        credentials {
            username = gprUser
            password = gprToken
        }
    }

    private val Project.gprToken
        get() = findProperty("gpr.key") as String? ?: System.getenv("GPR_TOKEN")

    private val Project.gprUser
        get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
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