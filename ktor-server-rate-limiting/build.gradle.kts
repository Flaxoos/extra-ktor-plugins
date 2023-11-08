import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.commonTestDependencies
import io.github.flaxoos.ktor.extensions.targetNative
import io.github.flaxoos.ktor.jvmMainDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    targetNative()
    sourceSets {
        commonMainDependencies {
            implementation(projects.common)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.call.id)
            implementation(libs.ktor.server.double.receive)
        }
        commonTestDependencies {
            implementation(libs.ktor.server.host.common)
            implementation(libs.ktor.server.call.id)
        }
        jvmMainDependencies {
            implementation(libs.ktor.server.call.logging.jvm)
        }
    }
}

koverReport {
    defaults {
        verify {
            rule {
                isEnabled = true
                minBound(70)
            }
            onCheck = true
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xuse-ir"
    }
}
