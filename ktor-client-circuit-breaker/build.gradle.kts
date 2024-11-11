import io.github.flaxoos.ktor.extensions.targetNative

plugins {
    id("ktor-client-plugin-conventions")
}
kotlin {
    targetNative()
}

tasks.named("compileTestKotlinNative") {
    enabled = false
}
