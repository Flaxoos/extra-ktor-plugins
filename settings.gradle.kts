rootProject.name = "extra-ktor-plugins"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.15.1")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include("ktor-server-rate-limiting")
include("ktor-server-kafka")
include("ktor-client-circuit-breaker")
