import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

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