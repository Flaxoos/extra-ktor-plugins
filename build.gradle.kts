import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED

plugins {
    id("conventions")
}

dependencies {
    kover(projects.ktorServerRateLimiting)
    kover(projects.ktorClientCircuitBreaker)
    kover(projects.ktorServerKafka)
}

subprojects {
    tasks.find { it.name == "build" }?.dependsOn(tasks.ktlintFormat)
}

tasks.withType(Test::class) {
    testLogging {
        events(FAILED)
        exceptionFormat = SHORT

        debug {
            events(*TestLogEvent.values())
            exceptionFormat = FULL
        }

        info.events(FAILED, PASSED)
    }
}

tasks.register("publishToMavenLocalWithShadowedJvm") {
    group = "publishing"
    dependsOn(
        subprojects.map { subproject ->
            listOf(
                subproject.tasks.publishKotlinMultiplatformPublicationToMavenLocal,
                subproject.tasks.publishShadowJvmPublicationToMavenLocal,
                subproject.tasks.publishNativePublicationToMavenLocal
            )
        }
    )
}

tasks.register("publishWithShadowedJvm") {
    group = "publishing"
    dependsOn(
        subprojects.map { subproject ->
            listOf(
                subproject.tasks.publishKotlinMultiplatformPublicationToMavenLocal,
                subproject.tasks.publishShadowJvmPublicationToMavenLocal,
                subproject.tasks.publishNativePublicationToMavenLocal,
                subproject.tasks.publishKotlinMultiplatformPublicationToGitHubPackagesRepository,
                subproject.tasks.publishShadowJvmPublicationToGitHubPackagesRepository,
                subproject.tasks.publishNativePublicationToGitHubPackagesRepository
            )
        }
    )
}
