plugins {
    id("conventions")
}

dependencies {
    kover(projects.ktorServerRateLimiting)
    kover(projects.ktorClientCircuitBreaker)
    kover(projects.ktorServerKafka)
}
tasks.register("publishAllToMavenLocal") {
    dependsOn(
        subprojects.filter { subproject -> subproject.name != projects.examples.name }
            .map { subproject ->
                listOf(
                    subproject.tasks.publishKotlinMultiplatformPublicationToMavenLocal,
                    subproject.tasks.publishShadowJvmPublicationToMavenLocal,
                    subproject.tasks.publishNativePublicationToMavenLocal,
                )
            },
    )
}
