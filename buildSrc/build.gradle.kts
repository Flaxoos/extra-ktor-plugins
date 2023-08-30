plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://maven.pkg.github.com/idoflax/flax-gradle-plugins")
        credentials {
            username = gprUser
            password = gprToken
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
            implementationClass = "io.flax.ktor.Conventions"
        }

        create("ktor-server-plugin-conventions") {
            id = "ktor-server-plugin-conventions"
            implementationClass = "io.flax.ktor.KtorServerPluginConventions"
        }

        create("ktor-client-plugin-conventions") {
            id = "ktor-client-plugin-conventions"
            implementationClass = "io.flax.ktor.KtorClientPluginConventions"
        }
    }
}

val Project.gprToken
    get() = findProperty("gpr.key") as String? ?: System.getenv("GPR_TOKEN")

val Project.gprUser
    get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
