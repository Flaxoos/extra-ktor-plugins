import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import io.github.flaxoos.ktor.extensions.jreleaserGpgPassphrase
import io.github.flaxoos.ktor.extensions.jreleaserGpgPublicKey
import io.github.flaxoos.ktor.extensions.jreleaserGpgSecretKey
import io.github.flaxoos.ktor.extensions.mcPassword
import io.github.flaxoos.ktor.extensions.mcUsername
import org.eclipse.jgit.api.Git
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogging
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter
import org.jreleaser.gradle.plugin.dsl.deploy.maven.MavenCentralMavenDeployer
import ru.vyarus.gradle.plugin.python.PythonExtension
import java.time.Instant.ofEpochSecond

plugins {
    base
    id(
        libs.plugins.dokka
            .get()
            .pluginId,
    )
    id(
        libs.plugins.jreleaser
            .get()
            .pluginId,
    )
    alias(libs.plugins.mkdocs.build)
    alias(libs.plugins.axion.release)
}

// set version based on conventional commit history
scmVersion {
    unshallowRepoOnCI.set(false)
    tag {
        prefix.set("v")
        versionSeparator.set("")
    }

    // Use predefined version creator that handles SNAPSHOT automatically
    versionCreator("simple")

    // Custom snapshot creator that respects release.mode property
    snapshotCreator { version, position ->
        val releaseMode = project.findProperty("release.mode")?.toString()
        when (releaseMode) {
            "release" -> "" // No suffix for production releases
            "snapshot", null -> "-SNAPSHOT" // Default to snapshot
            else -> "-SNAPSHOT"
        }
    }

    // For local development and workflow control, disable tag creation unless explicitly requested
    // This prevents markNextVersion from creating unwanted tags
    checks {
        uncommittedChanges.set(false)
    }

    // Custom version incrementer based on conventional commits
    versionIncrementer { context ->
        val git = Git.open(project.rootDir)
        try {
            val lastTagDesc =
                try {
                    git.describe().setTags(true).call()
                } catch (_: Exception) {
                    null
                }
            val lastTagName = lastTagDesc?.substringBefore("-")
            logger.quiet("[VersionIncrementer] Git describe result: $lastTagDesc")
            logger.quiet("[VersionIncrementer] Last tag name: $lastTagName")

            // Check if we have any tags at all
            val allTags = git.tagList().call()
            logger.quiet("[VersionIncrementer] Total tags in repository: ${allTags.size}")
            if (allTags.isNotEmpty()) {
                logger.quiet("[VersionIncrementer] Available tags: ${allTags.map { it.name.substringAfterLast("/") }}")
            }
            val repo = git.repository
            val head = repo.resolve("HEAD")
            val lastTagCommit = lastTagName?.let { repo.resolve("refs/tags/$it^{commit}") }

            logger.quiet("[VersionIncrementer] HEAD commit: $head")
            logger.quiet("[VersionIncrementer] Last tag commit: $lastTagCommit")

            val commits =
                if (lastTagCommit != null && head != null) {
                    logger.quiet("[VersionIncrementer] Getting commits between tag $lastTagName and HEAD")
                    git
                        .log()
                        .addRange(lastTagCommit, head)
                        .call()
                        .toList()
                } else if (allTags.isEmpty()) {
                    // If no tags exist at all, this is likely the first release
                    // Only look at commits since project started being versioned conventionally
                    logger.quiet("[VersionIncrementer] No tags found - treating as first release, analyzing recent commits only")
                    if (head != null) {
                        git
                            .log()
                            .add(head)
                            .setMaxCount(10) // Only look at last 10 commits for first release
                            .call()
                            .toList()
                    } else {
                        logger.quiet("[VersionIncrementer] No HEAD found, repository appears empty")
                        emptyList()
                    }
                } else {
                    // Tags exist but we couldn't resolve the last one
                    logger.quiet("[VersionIncrementer] Tags exist but couldn't resolve last tag, getting all commits from HEAD")
                    if (head != null) {
                        git
                            .log()
                            .add(head)
                            .call()
                            .toList()
                    } else {
                        logger.quiet("[VersionIncrementer] No HEAD found, repository appears empty")
                        emptyList()
                    }
                }

            logger.quiet("[VersionIncrementer] Found ${commits.size} commits since last tag")
            if (commits.isNotEmpty()) {
                logger.quiet(
                    "[VersionIncrementer] Commit range: ${
                        commits.last().name.substring(
                            0,
                            7,
                        )
                    }..${commits.first().name.substring(0, 7)}",
                )
            }

            var hasMajor = false
            var hasMinor = false
            var hasPatch = false

            val typeRegex = Regex("""^(?<type>\w+)(\([^)]*\))?(?<bang>!)?:\s""", RegexOption.IGNORE_CASE)
            val breakingFooter = Regex("""(?im)^BREAKING[ -]CHANGE:""")

            for (commit in commits) {
                val full = commit.fullMessage.trim()
                val subject = full.lineSequence().firstOrNull().orEmpty()
                val m = typeRegex.find(subject)
                val type =
                    m
                        ?.groups
                        ?.get("type")
                        ?.value
                        ?.lowercase()
                val bang = m?.groups?.get("bang") != null
                val breaking = bang || breakingFooter.containsMatchIn(full)

                logger.quiet("[VersionIncrementer] Analyzing commit: [${ofEpochSecond(commit.commitTime.toLong())}] $subject")
                logger.quiet("[VersionIncrementer]   Type: $type, Breaking: $breaking")

                when {
                    breaking -> {
                        hasMajor = true
                        logger.quiet("[VersionIncrementer]   → Triggers MAJOR version bump (breaking change)")
                    }
                    type == "feat" -> {
                        hasMinor = true
                        logger.quiet("[VersionIncrementer]   → Triggers MINOR version bump (new feature)")
                    }
                    type in
                        setOf(
                            "fix",
                            "perf",
                            "refactor",
                            "revert",
                            "docs",
                            "build",
                            "chore",
                            "test",
                            "style",
                            "ci",
                            "task",
                        )
                    -> {
                        hasPatch = true
                        logger.quiet("[VersionIncrementer]   → Triggers PATCH version bump ($type)")
                    }

                    else -> {
                        logger.quiet("[VersionIncrementer]   → No version impact")
                    }
                }
            }

            val v = context.currentVersion
            if (commits.isEmpty()) {
                logger.quiet("[VersionIncrementer] No commits since last tag → keeping version $v")
                return@versionIncrementer v
            }

            logger.quiet("[VersionIncrementer] Version bump analysis: major=$hasMajor, minor=$hasMinor, patch=$hasPatch")

            val newVersion =
                when {
                    hasMajor -> {
                        val next = if (v.majorVersion() == 0L) v.nextMinorVersion() else v.nextMajorVersion()
                        logger.quiet(
                            "[VersionIncrementer] MAJOR version bump: $v → $next (0.x special handling: ${v.majorVersion() == 0L})",
                        )
                        next
                    }

                    hasMinor -> {
                        val next = v.nextMinorVersion()
                        logger.quiet("[VersionIncrementer] MINOR version bump: $v → $next")
                        next
                    }

                    hasPatch -> {
                        val next = v.nextPatchVersion()
                        logger.quiet("[VersionIncrementer] PATCH version bump: $v → $next")
                        next
                    }

                    else -> {
                        val next = v.nextPatchVersion()
                        logger.quiet(
                            "[VersionIncrementer] Default PATCH version bump (commits present but no conventional signal): $v → $next",
                        )
                        next
                    }
                }

            newVersion
        } finally {
            git.close()
        }
    }
}
version = scmVersion.version

allprojects {
    group = "io.github.flaxoos"
    version = rootProject.version
}

jreleaser {
    // Configure the project
    project {
        name.set("${rootProject.name}: ${project.name}")
        description.set(
            "This project provides a suite of feature-rich, efficient, and highly customizable plugins for your Ktor Server or Client, crafted in Kotlin, available for multiplatform.",
        )
        longDescription.set(
            "A comprehensive collection of Ktor plugins including rate limiting, Kafka integration, circuit breaker patterns, and distributed task scheduling. Built with Kotlin Multiplatform support for JVM, Native, and JS platforms.",
        )
        authors.add("Ido Flax")
        license.set("Apache-2.0")
        copyright.set("Copyright (c) 2023 Ido Flax")
        authors.add("Ido Flax")
        maintainers.add("Ido Flax")
        links {
            homepage.set("https://github.com/Flaxoos/extra-ktor-plugins")
            documentation.set("https://flaxoos.github.io/extra-ktor-plugins/")
            license.set("https://opensource.org/license/apache-2-0")
            donation.set("https://github.com/sponsors/Flaxoos")
            contact.set("Kotlin Slack - Ido Flax. idoflax@gmail.com")
        }
        inceptionYear.set("2023")
    }
    // Sign the release
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        publicKey.set(jreleaserGpgPublicKey)
        secretKey.set(jreleaserGpgSecretKey)
        passphrase.set(jreleaserGpgPassphrase)
    }
    // Deploy to Maven Central
    deploy {
        maven {
            mavenCentral {
                create(
                    "flaxoos-extra-ktor-plugins",
                    closureOf<MavenCentralMavenDeployer> {
                        active.set(org.jreleaser.model.Active.ALWAYS)
                        url.set("https://central.sonatype.com/api/v1/publisher")
                        stagingRepository(
                            layout.buildDirectory
                                .dir("staging-deploy")
                                .get()
                                .asFile.absolutePath,
                        )

                        username.set(mcUsername)
                        password.set(mcPassword)

                        connectTimeout.set(20)
                        readTimeout.set(60)
                        retryDelay.set(5)
                        maxRetries.set(3)
                    },
                )
            }
        }
    }
    // Create a release in GitHub
    release {
        github {
            enabled.set(true)
            repoOwner.set("Flaxoos")
            name.set("extra-ktor-plugins")
            tagName.set("v{{projectVersion}}")
            releaseName.set("v{{projectVersion}}")
            overwrite.set(true)
            update {
                enabled.set(true)
            }
            changelog {
                enabled.set(true)
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                contributors {
                    enabled.set(false)
                }
                hide {
                    categories.add("merge")
                    contributors.add("GitHub")
                }
            }
        }
    }
}

tasks.jreleaserAssemble {
    dependsOn(
        subprojects.flatMap { sp ->
            sp.tasks.matching { it.name == "publishAllPublicationsToLocalStagingRepository" }
        },
    )
}

subprojects {
    tasks.find { it.name == "build" }?.dependsOn(tasks.named("ktlintFormat"))

    tasks.withType(Test::class) {
        testLogging {
            info {
                testDetails()
            }
        }
    }

    extensions.findByType(GradleEnterpriseExtension::class)?.apply {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

mkdocs {
    sourcesDir = layout.projectDirectory.dir("documentation/mkdocs").toString()
    python.scope = PythonExtension.Scope.USER
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaCollectorTask>().configureEach {
    outputDirectory.set(file("$rootDir/documentation/mkdocs/docs/dokka"))
}

fun TestLogging.testDetails() {
    events = setOf(PASSED, SKIPPED, FAILED)
    showStandardStreams = true
    exceptionFormat = SHORT
    stackTraceFilters = setOf(TestStackTraceFilter.GROOVY)
}
