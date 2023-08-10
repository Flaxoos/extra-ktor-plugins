plugins {
    id("conventions")
}

kotlin {
    dependencies {
        kover(projects.ktorServerRateLimiting)
        kover(projects.ktorClientCircuitBreaker)
    }
}
