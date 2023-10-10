import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    id(libs.plugins.kover.asProvider().get().pluginId)
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

dependencies {
    kover(projects.ktorServerRateLimiting)
    kover(projects.ktorClientCircuitBreaker)
    kover(projects.ktorServerKafka)
}

subprojects {
    tasks.find { it.name == "build" }?.dependsOn(tasks.named("ktlintFormat"))
}

tasks.withType(Test::class) {
    testLogging {
        events(FAILED)
        exceptionFormat = SHORT

        debug {
            events(*TestLogEvent.values())
            exceptionFormat = FULL
        }

        info.events(FAILED, PASSED, SKIPPED)
    }
}

//tasks.register("publishToMavenLocalWithShadowedJvm") {
//    group = "publishing"
//    dependsOn(
//        subprojects.map { subproject ->
//            subproject.tasks.withType<PublishToMavenLocal>()
//                .filterNot { it.name.contains("jvm", ignoreCase = true) }
//                .plus(subproject.tasks.publishShadowJvmPublicationToMavenLocal)
//        }
//    )
//}
//
//tasks.register("publishWithShadowedJvm") {
//    group = "publishing"
//    dependsOn(
//        subprojects.map { subproject ->
//            subproject.tasks.withType<AbstractPublishToMaven>()
//                .filterNot { it.name.contains("jvm", ignoreCase = true) }
//                .plus(subproject.tasks.publishShadowJvmPublicationToMavenLocal)
//        }
//    )
//}
