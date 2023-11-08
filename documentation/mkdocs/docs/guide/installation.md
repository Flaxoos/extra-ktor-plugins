# Installation

The libraries are published to maven central, see above for the latest version
```kotlin
dependencies {
    implementation("io.github.flaxoos:ktor-server-kafka:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-server-task-scheduling-$module:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-server-rate-limiting:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-client-circuit-breaker:$ktor_plugins_version")
}
```