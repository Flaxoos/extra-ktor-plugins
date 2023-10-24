import io.github.flaxoos.ktor.extensions.enableContextReceivers

plugins {
    id("ktor-server-plugin-conventions")
}
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.ktorServerTaskScheduler.redisClient)
                api(libs.krontab)
                implementation(libs.uuid)
                implementation("org.mobilenativefoundation.store:store5:5.0.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.quartz-scheduler:quartz:2.3.2")
            }
        }
    }
}