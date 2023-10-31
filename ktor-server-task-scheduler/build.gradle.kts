plugins {
    id("ktor-server-plugin-conventions")
    kotlin("plugin.serialization") version libs.versions.kotlin.asProvider().get()
}
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.ktorServerTaskScheduler.redisClient)
                implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
                implementation("org.mongodb:bson-kotlinx:4.11.0")
//                implementation("com.github.jershell:kbson:0.5.0")
                api(libs.krontab)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.exposed.core)
                implementation(libs.exposed.jdbc)
                implementation(libs.exposed.dao)
                implementation(libs.exposed.kotlin.datetime)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.postgresql)
                implementation(libs.kotest.extensions.testcontainers)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.redis)
                implementation(libs.testcontainers.mongodb)
                implementation(libs.testcontainers.postgres)
                implementation(libs.kotest.property)
            }
        }
    }
}
