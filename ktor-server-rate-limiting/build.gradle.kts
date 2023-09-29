import io.github.flaxoos.ktor.library
import io.github.flaxoos.ktor.projectDependencies

plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.ktor.server.host.common)
            }
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
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}