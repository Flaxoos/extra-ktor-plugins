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

include("common")
include("ktor-server-rate-limiting")
include("ktor-server-kafka")
include("ktor-client-circuit-breaker")
include("ktor-server-task-scheduling")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-core")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-core:test")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-jdbc")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-mongodb")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-redis")
include("ktor-server-task-scheduling:ktor-server-task-scheduling-redis:redis-client")
include("ktor-server-task-scheduling:common-test")
