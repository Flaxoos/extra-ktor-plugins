import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.jvmMainDependencies
import io.github.flaxoos.ktor.jvmTestDependencies

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
    targetJvm(project)
    sourceSets {
        jvmMainDependencies {
            api(libs.kafka.clients)
            api(libs.avro4k.core)
            api(libs.kafka.avro.serializer)
            api(libs.kafka.schema.registry.client)
            api(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.java)
            implementation(libs.ktor.client.contentNegotiation)
        }
        jvmTestDependencies {
            implementation(project.dependencies.platform(libs.testcontainers.bom.get()))
            implementation(libs.kotest.extensions.testcontainers)
            implementation(libs.testcontainers.kafka)
            implementation(libs.ktor.server.contentNegotiation)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
