# Ktor Server Multiplatform Plugins Library
![Build status](https://github.com/flaxoos/flax-ktor-plugins/actions/workflows/build-and-publish-main.yml/badge.svg?event=push)
![Language](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin)
<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/94.42-green?logo=kotlin&label=koverage&style=flat)</a>

This project provides a suite of useful, efficient, and customizable plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.

## Features

These plugins offer a wide range of features designed to provide additional layers of control, security, and utility to your server or client.

## **Server Plugins**

| Plugin               | Platform Support | Description                                                                                                                                                                                        |
|----------------------|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Kafka Plugin         | JVM              | Sets up a Kafka client admin, producer, and consumer using a dedicated DSL. Allows consumption logic definition during the installation phase.                                                     |
| Rate Limiting Plugin | JVM / Native     | Limits the number of requests a client can make within a specific time window. Offers whitelist and blacklist features for hosts, principals, and user-agents. Custom responses can be configured. |

## Supported Platforms for Plugins

| Plugin                     | Platform Support | Description                                                                                                                                                                                                      |
|----------------------------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Circuit Breaker Plugin** | JVM / Native     | Enhances system resilience by halting requests to failing services once a defined error threshold is reached. Automatically switches between open and closed states based on the health of the targeted service. |


## Usage

Please refer to the readme of the relevant plugin subproject

## Installation

This library is available via a JAR file that can be included in your Kotlin or Java project, or as a library to be included in your Gradle/Maven project.

## Contributing

Contributions are always welcome! If you have an idea for a plugin or want to improve an existing one, feel free to fork this repository and submit a pull request.

### Notes for contributors
If you want to build this locally, there is a dependency on a custom gradle plugin for adding the test coverage badge. As of 30/9/2023, it's gradle plugin portal status is still pending, so the plugin is available via github packages. Because github public packages still requires a token for reading, and token can't and shouldn't be committed, please contact me at idoflax@gmail.com and I will provide you the token. Alternatively, you can pull the plugin project from [here](https://github.com/Flaxoos/flax-gradle-plugins) and build it yourself, or just disable the kover-badge plugin, it won't affect the build.
