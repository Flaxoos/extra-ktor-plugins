# 🛠 Ktor Multiplatform Plugins Library
![Build Status](https://github.com/flaxoos/flax-ktor-plugins/actions/workflows/build-and-publish-main.yml/badge.svg?event=push) ![Language: Kotlin](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin) [![Koverage: 94.42%](https://img.shields.io/badge/94.42-green?logo=kotlin&label=koverage&style=flat)](file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html)

**This project provides a suite of feature-rich, efficient, and highly customizable plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.**

---

## 🌟 Features
These plugins offer a wide range of functionalities designed to provide additional layers of control, security, and utility to your server or client.

---

### 🖥️ **Server Plugins**

| 📦 Plugin              | 🎯 Supported Platforms | 📜 Description                                                                                                                                                                                       |
|------------------------|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Kafka Plugin**       | ☕ JVM                 | Sets up a Kafka client admin, producer, and consumer using a dedicated DSL. Allows consumption logic definition during the installation phase.                                                      |
| **Rate Limiting Plugin**| ☕ JVM / 💾 Native      | Limits the number of requests a client can make within a specific time window. Offers whitelist and blacklist features for hosts, principals, and user-agents. Custom responses can be configured. |

---

### 🖱️ **Client Plugins**

| 📦 Plugin                     | 🎯 Supported Platforms | 📜 Description                                                                                                                                                                                                     |
|-------------------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Circuit Breaker Plugin**    | ☕ JVM / 💾 Native      | Enhances system resilience by halting requests to failing services once a defined error threshold is reached. Automatically switches between open and closed states based on the health of the targeted service. |

---


## Usage

Please refer to the readme of the relevant plugin subproject

## Installation
The libraries are published to the packages in this repository ➜
```kotlin
dependencies {
    implementation("io.github.flaxoos:ktor-server-kafka:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-server-rate-limiting:$ktor_plugins_version")
    implementation("io.github.flaxoos:ktor-client-circuit-breaker:$ktor_plugins_version")
}
```

## Contributing

Contributions are always welcome! If you have an idea for a plugin or want to improve an existing one, feel free to fork this repository and submit a pull request.

### Notes for contributors
If you want to build this locally, there is a dependency on a custom gradle plugin for adding the test coverage badge. As of 30/9/2023, it's gradle plugin portal status is still pending, so the plugin is available via github packages. Because github public packages still requires a token for reading, and token can't and shouldn't be committed, please contact me at idoflax@gmail.com and I will provide you the token. Alternatively, you can pull the plugin project from [here](https://github.com/Flaxoos/flax-gradle-plugins) and build it yourself, or just disable the kover-badge plugin, it won't affect the build.
