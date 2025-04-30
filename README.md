# [![Ktor](https://avatars.githubusercontent.com/u/28214161?s=40&v=4.svg)](https://github.com/ktorio/ktor) Extra Ktor Plugins

![Build Status](https://img.shields.io/github/actions/workflow/status/flaxoos/extra-ktor-plugins/build-and-publish-main.yml?event=push&logo=githubactions&style=for-the-badge)
[![Maven-central](https://img.shields.io/maven-central/v/io.github.flaxoos/ktor-server-kafka?style=for-the-badge&logo=apachemaven)](https://search.maven.org/search?q=io.github.flaxoos%20AND%20ktor)
![Language: Kotlin](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin&style=for-the-badge)
[![Koverage: 94.42%](https://img.shields.io/badge/94.42-green?logo=kotlin&label=koverage&style=for-the-badge)](file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html)
[![Docs](https://custom-icon-badges.demolab.com/badge/Pages-blue.svg?label=Docs&logo=github&logoColor=white?icon=githubpages&style=for-the-badge)](https://github.com/Flaxoos/extra-ktor-plugins/actions/workflows/pages/pages-build-deployment)
[![Awesome Kotlin Badge](https://custom-icon-badges.demolab.com/badge/awesome-kotlin-orange.svg?labelColor=blue&style=for-the-badge)](https://github.com/KotlinBy/awesome-kotlin)
---

### Feature-rich, efficient, and highly customizable plugins for your Multiplatform Ktor Server or Client

---

### **Server Plugins**

| Plugin                                             | Supported Platforms | Description                                                                                                                                                                     |
|----------------------------------------------------|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **[Kafka](ktor-server-kafka)**                     | **JVM**             | Sets up a Kafka client admin, producer, and consumer using a dedicated DSL. Allows consumption logic definition during the installation phase.                                  |
| **[Task Scheduling](ktor-server-task-scheduling)** | **JVM / Native^**   | Task scheduling for distributed servers with various and extendable task management strategies                                                                                  |
| **[Rate Limiting](ktor-server-rate-limiting)**     | **JVM / Native**    | Highly configurable rate limiter with offering different startegies, request weighting, blacklisting and whitelisting of requests based on authentication, host and user agents |

### **Client Plugins**

| Plugin                                             | Supported Platforms   | Description                                                                                                                                                                                                      |
|----------------------------------------------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **[Circuit Breaker](ktor-client-circuit-breaker)** | **JVM / Native / JS** | Enhances system resilience by halting requests to failing services once a defined error threshold is reached. Automatically switches between open and closed states based on the health of the targeted service. |

Note that `^` means issues with native binary dependencies, feel free to pull the project and publish locally

---

## Usage

Pleas see [Documentation](https://flaxoos.github.io/extra-ktor-plugins/)

## Installation

The libraries are published to maven central, see above for the latest version

```kotlin
dependencies {
    implementation("io.github.flaxoos:ktor-server-kafka:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-server-task-scheduling-$module:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-server-rate-limiting:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-client-circuit-breaker:$ktor_plugins_version")
}
```

## Examples:

See [examples repository](https://github.com/Flaxoos/flax-ktor-plugins-examples)

## Contributing

Contributions are always welcome! If you have an idea for a plugin or want to improve an existing one, feel free to fork
this repository and submit a pull request.

## Sponsership

If you find this project useful, feel free to use the sponser button to support it ❤️ ->
