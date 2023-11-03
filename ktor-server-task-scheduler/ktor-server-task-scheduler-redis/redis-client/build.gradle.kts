import io.github.flaxoos.ktor.commonMainDependencies
import io.github.flaxoos.ktor.commonTestDependencies

import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative
import io.github.flaxoos.ktor.jvmMainDependencies
import io.github.flaxoos.ktor.jvmTestDependencies

plugins {
    kotlin("multiplatform")
    id(libs.plugins.kotest.get().pluginId)
    id(libs.plugins.loggingCapabilities.get().pluginId)
}

kotlin {
    targetJvm()
    targetNative("redis-client", configure = {
        compilations.getByName("main") {
            cinterops {
                val hiredis by creating {
                    defFile("src/nativeInterop/cinterop/hiredis.def")  // Point this to your .def file
                    compilerOpts("-Isrc/nativeInterop/hiredis", "-o knedis")
                    includeDirs("src/nativeInterop/hiredis")
                }
            }
        }
    })
    sourceSets {
        commonMainDependencies {
            implementation(projects.common)
            implementation(libs.kotlin.logging)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.reflect)
        }
        commonTestDependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
        jvmMainDependencies {
            api("io.netty:netty-transport:4.1.91.Final")
            api("io.netty:netty-codec-redis:4.1.99.Final")
            api(libs.kreds)
        }
        jvmTestDependencies {
            implementation(libs.kotest.runner.junit5)
            implementation(libs.mockk)
            implementation(libs.mockk.agent.jvm)
            implementation(libs.logback.classic)
            implementation("io.netty:netty-transport:4.1.91.Final")
            implementation("io.netty:netty-codec-redis:4.1.99.Final")
        }
    }
}

loggingCapabilities {
    enforceLogback()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
