# Ktor Server Multiplatform Plugins Library
![Build status](https://github.com/idoflax/flax-ktor-plugins/actions/workflows/build-and-publish-main.yml/badge.svg?event=push)
![Language](https://img.shields.io/github/languages/top/idoflax/flax-ktor-plugins?color=blue&logo=kotlin)
<a href="file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html">![koverage](https://img.shields.io/badge/91.7-green?logo=kotlin&label=koverage&style=flat)</a>

This project provides a suite of useful, efficient, and customizable plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.

## Features

These plugins offer a wide range of features designed to provide additional layers of control, security, and utility to your server or client.

* **Rate Limiting Plugin**: Limit the number of requests a client can make in a specific time window. This plugin also offers whitelist and blacklist features for hosts, principals, and user-agents. You can configure custom responses for blacklisted callers or when the rate limit is exceeded.

* **Circuit Breaker Plugin**:  Enhance system resilience by halting requests to failing services once a defined error threshold is reached. The plugin automatically switches between open and closed states based on the health of the targeted service, allowing it to recover. Configuration options include failure thresholds, fallback responses, and reset intervals, making it a pivotal tool in ensuring smooth system operations.

## Usage

Please refer to the readme of the relevant plugin subproject

Detailed usage is available in the tests and in [examples repository](https://github.com/idoflax/flax-ktor-plugins/ktor-plugins-examples)

## Installation

This library is available via a JAR file that can be included in your Kotlin or Java project, or as a library to be included in your Gradle/Maven project.

## Contributing

Contributions are always welcome! If you have an idea for a plugin or want to improve an existing one, feel free to fork this repository and submit a pull request.
