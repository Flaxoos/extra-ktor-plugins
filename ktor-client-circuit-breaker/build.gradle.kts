import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    id("ktor-client-plugin-conventions")
}
kotlin {
    targetNative()
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.arrow.resilience)
            }
        }
    }
}

tasks.named("compileTestKotlinNative") {
    enabled = false
}
