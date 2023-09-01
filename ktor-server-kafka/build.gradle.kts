

plugins {
    id("ktor-server-plugin-conventions")
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.repository.redhat.com/earlyaccess/all/")
    }
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

tasks.matching { it.name.contains("native", ignoreCase = true) }.configureEach {
    enabled = false
}

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.kafka.clients)
                api(libs.kafka.streams)
                implementation(libs.kafka.schema.registry.client)
                implementation(libs.kafka.avro.serializer)
                implementation(libs.avro4k.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        jvmTest {
            dependencies {
                implementation(platform(libs.testcontainers.bom.get()))
                implementation(libs.kotest.extensions.testcontainers)
                implementation(libs.kotest.extensions.testcontainers.kafka)
                implementation(libs.testcontainers.kafka)
                implementation(libs.ktor.server.contentNegotiation)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
}
