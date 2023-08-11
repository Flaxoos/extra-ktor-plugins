plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `version-catalog`
}

kotlin{
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.atomicfu.gradlePlugin)
    implementation(libs.loggingCapabilities.gradlePlugin)
    implementation(libs.kotestFrameworkMultiplatform.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
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

