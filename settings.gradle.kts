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
include("ktor-server-task-scheduler")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-core")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-core:test")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-jdbc")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-mongodb")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-redis")
include("ktor-server-task-scheduler:ktor-server-task-scheduler-redis:redis-client")
include("ktor-server-task-scheduler:common-test")
