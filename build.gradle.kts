plugins {
    id("conventions")
}

dependencies {
    kover(projects.ktorServerRateLimiting)
    kover(projects.ktorClientCircuitBreaker)
}
