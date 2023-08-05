rootProject.name = "flax-ktor-plugins"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("ktor-rate-limiting", "ktor-graphql")
