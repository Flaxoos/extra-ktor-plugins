import io.github.flaxoos.ktor.extensions.nativeTarget

plugins {
    kotlin("multiplatform")
}
kotlin {
    nativeTarget("redis-client", configure = {
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
    jvm {
        jvmToolchain(libs.versions.java.get().toInt())
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.common)
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kreds)
            }
        }
    }
}
