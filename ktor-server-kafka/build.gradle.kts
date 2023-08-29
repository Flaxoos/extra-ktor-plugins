plugins {
    id("ktor-server-plugin-conventions")
    alias(libs.plugins.kotlin.serialization)
}

version = "0.0.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.repository.redhat.com/earlyaccess/all/")
    }
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}
kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                api(libs.kafka.clients)
                api(libs.kafka.streams)
                api(libs.kafka.schema.registry.client)
                api(libs.kafka.avro.serializer)
                api(libs.avro4k.core)
                implementation(libs.arrow.core)
                implementation(libs.arrow.fx.coroutines)
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
