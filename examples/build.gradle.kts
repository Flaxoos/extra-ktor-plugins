

plugins {
    kotlin("jvm")
//    id("io.ktor.plugin") version libs.versions.ktor
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenLocal()
    mavenCentral()
//    maven {
//        url = uri("https://packages.confluent.io/maven/")
//    }
//    maven {
//        name = "GitHubPackages"
//        url =
//            URI("https://maven.pkg.github.com/flaxoos/flax-ktor-plugins")
//        credentials {
//            username = gprUser
//            password = gprReadToken
//        }
//    }
}
dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
    implementation("io.github.flaxoos:ktor-server-kafka:${property("VERSION")}")
    implementation("io.github.flaxoos:ktor-server-rate-limiting:${property("VERSION")}")
    implementation("io.github.flaxoos:ktor-client-circuit-breaker:${property("VERSION")}")
}

application {
    mainClass.set("flaxoos.github.io.ApplicationKt")
}

val Project.gprReadToken: String?
    get() = findProperty("gpr.read.key") as String? ?: System.getenv("GPR_READ_TOKEN")

val Project.gprUser: String?
    get() = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
