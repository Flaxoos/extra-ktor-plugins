package io.github.flaxoos.ktor.extensions

import io.github.flaxoos.ktor.ProjectPropertyDelegate
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

context(Project)
fun MavenArtifactRepository.gprWriteCredentials() {
    credentials {
        username = gprUser
        password = gprWriteKey
    }
}

context(Project)
fun MavenArtifactRepository.gprReadCredentials() {
    credentials {
        username = gprUser
        password = gprReadKey
    }
}

context(Project)
fun MavenArtifactRepository.ossrhCredentials() {
    credentials {
        username = ossrhUsername
        password = ossrhPassword
    }
}

val Project.gprWriteKey: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.gprReadKey: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.gprUser: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.ossrhUsername: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.ossrhPassword: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.signingKeyBase64: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.signingKeyArmorBase64: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.signingPassword: String by ProjectPropertyDelegate.projectOrSystemEnv()