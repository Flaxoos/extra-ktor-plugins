import java.net.URI

val kotlin_version = "1.9.0"
val ktor_version: String by project
val logback_version: String by project
val kotlinx_datetime_version: String by project
val kotlinx_coroutines_version: String by project
val logging_version: String by project
val kotest_version = "5.6.2"

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("me.qoomon.git-versioning")
    id("io.kotest.multiplatform")
    id("org.jetbrains.kotlinx.kover")
    id("dev.jacomet.logging-capabilities")
}

group = "io.flax.ktor"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {

    jvm {
        jvmToolchain(11)
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
                implementation("io.ktor:ktor-server-auth:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
            }
        }

        @Suppress("UNUSED")
        val commonTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-test-host:$ktor_version")
                implementation("io.ktor:ktor-server-status-pages:$ktor_version")
                implementation("io.kotest:kotest-framework-engine:$kotest_version")
                implementation("io.kotest:kotest-assertions-core:$kotest_version")
                implementation("io.kotest:kotest-framework-engine:$kotest_version")
            }
        }

        @Suppress("UNUSED")
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotest_version")
            }
        }
    }
}

tasks.withType(Test::class) {
    useJUnitPlatform()
}

tasks.withType<Wrapper> {
    gradleVersion = "8.1.1"
    distributionType = Wrapper.DistributionType.BIN
}

koverReport {
    defaults {
        html { onCheck = true }
        verify {
            rule {
                isEnabled = true
                minBound(95)
            }
            onCheck = true
        }
    }
}


publishing {
    repositories {
        maven {
            createReleaseTag()
            name = "GitHubPackages"
            url =
                URI("https://maven.pkg.github.com/idoflax/${project.findProperty("github.repository.name") ?: project.name}")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}

/**
 * Delets the current tag and recreates it
 */
fun createReleaseTag() {
    val tagName = "release/${version}"
    try {
        runCommands("git", "tag", "-d", tagName)
    } catch (e: Exception) {
        logger.error(e.message)
    }
    runCommands("git", "status")
    runCommands("git", "tag", tagName)
}

/**
 * Run a command
 */
fun runCommands(vararg commands: String): String {
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