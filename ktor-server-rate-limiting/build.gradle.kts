plugins {
    id("ktor-server-plugin-conventions")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
            }
        }
    }
}
