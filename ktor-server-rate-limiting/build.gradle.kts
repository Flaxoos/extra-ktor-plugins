

plugins {
    id("ktor-server-plugin-conventions")
}

dependencies {
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.2")
    implementation("io.ktor:ktor-server-call-id-jvm:2.3.2")
    implementation("io.ktor:ktor-server-core-jvm:2.3.2")
    implementation("io.ktor:ktor-server-double-receive-jvm:2.3.2")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.server.double.receive)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.ktor.server.host.common)
                implementation(libs.ktor.server.call.id)
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
        freeCompilerArgs += "-Xcontext-receivers"
        freeCompilerArgs += "-Xuse-ir"
    }
}
