plugins {
    `kotlin-dsl`
    `java-library`
    `java-gradle-plugin`
    `version-catalog`
}

kotlin {
    jvmToolchain(
        libs.versions.java
            .get()
            .toInt(),
    )
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
    maven {
        url = uri("https://maven.pkg.github.com/flaxoos/flax-gradle-plugins")
        credentials {
            username = gprUser
            password = gprReadToken
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.atomicfu.gradlePlugin)
    implementation(libs.loggingCapabilities.gradlePlugin)
    implementation(libs.kotestFrameworkMultiplatform.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.ktor.client.cio)
    implementation(libs.kover.badge.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.gradle.release.gradlePlugin)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

gradlePlugin {
    plugins {
        create("conventions") {
            id = "conventions"
            implementationClass = "io.github.flaxoos.ktor.Conventions"
        }

        create("ktor-server-plugin-conventions") {
            id = "ktor-server-plugin-conventions"
            implementationClass = "io.github.flaxoos.ktor.KtorServerPluginConventions"
        }

        create("ktor-client-plugin-conventions") {
            id = "ktor-client-plugin-conventions"
            implementationClass = "io.github.flaxoos.ktor.KtorClientPluginConventions"
        }
    }
}

private val Project.gprWriteToken
    get() = findProperty("gpr.write.key") as String? ?: System.getenv("GPR_WRITE_TOKEN")

private val Project.gprReadToken
    get() = findProperty("gpr.read.key") as String? ?: System.getenv("GPR_READ_TOKEN")

private val Project.gprUser
    get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
