import io.github.flaxoos.ktor.extensions.configurePublishing
import io.github.flaxoos.ktor.extensions.targetJvm
import io.github.flaxoos.ktor.extensions.targetNative
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("signing")
    id(
        libs.plugins.dokka
            .get()
            .pluginId,
    )
}

kotlin {
    targetJvm(project)
    targetNative()
    // Check if running on macOS and macOS version is less than 15.0.1
    val macosVersion = System.getProperty("os.version")?.split(".")?.map { it.toIntOrNull() ?: 0 }
    val isMacosCompatible = macosVersion != null && macosVersion[0] < 15

    if (HostManager.hostIsMac && isMacosCompatible) {
        macosArm64("native-macos") {
            binaries {
                staticLib()
            }
        }
    }
}
configurePublishing()
