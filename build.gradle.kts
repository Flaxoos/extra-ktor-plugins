import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import io.github.flaxoos.ktor.ossrhPassword
import io.github.flaxoos.ktor.ossrhUsername
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    alias(libs.plugins.nexusPublish)
}

group = "io.github.flaxoos"
version = project.property("version") as String

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = ossrhUsername
            password = ossrhPassword
        }
    }
}

subprojects {
    tasks.find { it.name == "build" }?.dependsOn(tasks.named("ktlintFormat"))

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

    extensions.findByType(GradleEnterpriseExtension::class)?.apply {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

