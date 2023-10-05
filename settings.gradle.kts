rootProject.name = "flax-ktor-plugins"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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
