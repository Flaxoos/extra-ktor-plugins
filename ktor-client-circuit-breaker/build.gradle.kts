import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    id("ktor-client-plugin-conventions")
}
kotlin {
    targetNative()
}
