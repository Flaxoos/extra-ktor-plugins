plugins {
    id("ktor-server-plugin-conventions")
}
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.krontab)
            }
        }
    }
}