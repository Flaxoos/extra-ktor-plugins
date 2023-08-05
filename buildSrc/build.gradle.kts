plugins {
    `kotlin-dsl`
}
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.7.2")
    implementation("dev.jacomet.gradle.plugins:logging-capabilities:0.11.1")
    implementation("me.qoomon:gradle-git-versioning-plugin:6.4.2")
    implementation("io.kotest:kotest-framework-multiplatform-plugin-gradle:5.6.2")
}
