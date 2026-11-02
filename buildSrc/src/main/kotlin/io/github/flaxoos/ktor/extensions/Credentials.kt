package io.github.flaxoos.ktor.extensions

import io.github.flaxoos.ktor.ProjectPropertyDelegate
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

context(project: Project)
fun MavenArtifactRepository.gprReadCredentials() {
    credentials {
        username = project.gprUser
        password = project.gprReadToken
    }
}

val Project.gprReadToken: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.gprUser: String by ProjectPropertyDelegate.projectOrSystemEnv()

val Project.mcUsername: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.mcPassword: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.ossrhUsername: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.ossrhPassword: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.jreleaserGpgPublicKey: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.jreleaserGpgSecretKey: String by ProjectPropertyDelegate.projectOrSystemEnv()
val Project.jreleaserGpgPassphrase: String by ProjectPropertyDelegate.projectOrSystemEnv()
