# Release Guide

This document explains how to create releases for the extra-ktor-plugins project.

## Quick Start - How to Release

### 🔄 Automatic Snapshots
**No action needed!** Snapshots are built and staged locally:
- Push commits to `main` branch
- Wait for `build-main.yml` to succeed
- `release.yml` automatically triggers and creates a snapshot (e.g., `2.3.0-SNAPSHOT`)
- Artifacts are staged locally but **not published to Maven Central**

### 🚀 Production Release
1. Go to **Actions** tab in GitHub
2. Select **"Release And Publish"** workflow
3. Click **"Run workflow"**
4. Select **"release"** from the dropdown
5. Click **"Run workflow"** button

**Result:** Creates a production release (e.g., `2.3.0`) with:
- Clean version tag (`v2.3.0`)
- GitHub release with changelog
- Artifacts published to Maven Central (production)

### 📦 Manual Snapshot (Optional)
Same as production release, but select **"snapshot"** from the dropdown.

### Snapshot vs Release Details

The `release.yml` workflow decides whether to perform a snapshot or a release based on the type of trigger and the `release.mode` property:

- **Automatic triggers** (e.g., after successful `build-main.yml`) default to `release.mode=snapshot`, producing snapshot versions.
- **Manual triggers** allow selection between `snapshot` and `release` modes.
- The `release.mode` property controls version suffixes and deployment behavior:
    - `snapshot` mode appends `-SNAPSHOT` and stages artifacts locally without publishing to Maven Central.
    - `release` mode produces clean versions and publishes artifacts to Maven Central with GitHub release creation.

---

## How It Works

### Architecture Overview

The release system uses three main components working together:

```
Push to main → build-main.yml → release.yml (snapshot)
                    ↓
Manual trigger → release.yml (release/snapshot)
                    ↓
Tag push → release.yml (release)
```

### Component Roles

#### 1. **Axion Release Plugin** (`build.gradle.kts`)
**Role:** Version calculation and snapshot control

**Key Features:**
- Analyzes git commits using conventional commit patterns
- Calculates next version based on commit types:
  - `feat:` → Minor version bump
  - `fix:`, `perf:`, `refactor:`, etc. → Patch version bump
  - `BREAKING CHANGE:` → Major version bump
- Custom `snapshotCreator` respects `release.mode` property:
  - `release.mode=snapshot` → adds `-SNAPSHOT` suffix
  - `release.mode=release` → no suffix

**Configuration:**
```kotlin
snapshotCreator { version, position ->
    val releaseMode = project.findProperty("release.mode")?.toString()
    when (releaseMode) {
        "release" -> ""  // No suffix for production releases
        "snapshot", null -> "-SNAPSHOT"  // Default to snapshot
        else -> "-SNAPSHOT"
    }
}
```

#### 2. **Release Workflow** (`.github/workflows/release.yml`)
**Role:** Orchestrates the release process

**Triggers:**
- **`workflow_run`**: Automatic trigger after successful `build-main.yml`
- **`workflow_dispatch`**: Manual trigger with release type selection
- **`push: tags`**: Tag-based releases (backward compatibility)

**Key Steps:**
1. **Determine Release Context**: Identifies trigger type and sets release mode
2. **Generate Version**: Uses axion with appropriate `release.mode` property
3. **Build & Stage**: Compiles and prepares artifacts
4. **JReleaser Execution**: Conditional based on release type

**Conditional Logic:**
```yaml
- name: JReleaser Configuration
  run: |
    if [[ "${{ steps.version.outputs.is_snapshot }}" == "true" ]]; then
      ./gradlew jreleaserDeploy -Prelease.mode=snapshot
    else
      ./gradlew jreleaserFullRelease -Prelease.mode=release
    fi
```

#### 3. **JReleaser** (Maven Central + GitHub Releases)
**Role:** Publishes artifacts and creates releases

**Snapshot Mode (`jreleaserDeploy`):**
- Publishes to Maven Central staging repository
- No GitHub release created
- No tags created
- Snapshots are released to ossrh repository (maven central doesn't support snapshots)

**Release Mode (`jreleaserFullRelease`):**
- Publishes to Maven Central (production)
- Creates GitHub release with changelog
- Generates release tag

**Configuration:** Located in `build.gradle.kts` under `jreleaser` block

##### Snapshot Publishing (optional)

To enable snapshot publishing with JReleaser, you can add a snapshot deployer in Gradle configuration:

```kotlin
jreleaser {
    snapshot {
        active = org.jreleaser.model.Active.SNAPSHOT
        // configure snapshot deployer details here
    }
}
```

#### 4. **Build Main Workflow** (`.github/workflows/build-main.yml`)
**Role:** Quality gate for automatic snapshots

**Purpose:**
- Runs tests, builds, and validations
- Only triggers releases if all checks pass
- Prevents broken code from being published as snapshots

#### 5. **Test Workflows**
**Local Testing:** `test-release-config.sh`
- Validates version generation for both modes
- Ensures no unwanted tags are created
- Can be run locally before changes

**CI Testing:** `.github/workflows/test-release-config.yml`
- Automated validation in CI environment
- Tests both snapshot and release scenarios
- Manual trigger for validation

### Version Flow Examples

#### Snapshot Flow
```
1. Developer pushes fix to main
2. build-main.yml: ✅ Tests pass
3. release.yml triggered automatically
4. Axion: Analyzes commits → "fix" found → patch bump
5. Version: 2.2.1 → 2.2.2-SNAPSHOT
6. JReleaser: Publishes to staging repository
7. Result: 2.2.2-SNAPSHOT available for testing
```

#### Release Flow
```
1. Maintainer triggers workflow manually
2. Selects "release" type
3. Axion: Uses same commit analysis → 2.2.2
4. JReleaser: Full release process
5. Creates tag: v2.2.2
6. GitHub release with changelog
7. Publishes to Maven Central production
8. Result: 2.2.2 available publicly
```

### Conventional Commits Integration

The system relies on conventional commit messages for version calculation and now includes merge commits in the changelog generation using the settings `skipMergeCommits=false` and `includeUncategorized=true`. Non-conventional commit subjects such as merge commits will appear under the "Uncategorized" section of the changelog, while CI-related commits are filtered out to keep the changelog relevant and clean.

| Commit Type | Version Impact | Example |
|-------------|----------------|---------|
| `feat:` | Minor bump | `feat(auth): add OAuth support` |
| `fix:` | Patch bump | `fix(kafka): connection timeout issue` |
| `perf:` | Patch bump | `perf(rate-limit): optimize token bucket` |
| `BREAKING CHANGE:` | Major bump | `feat!: remove deprecated API` |
| `docs:`, `test:`, etc. | Patch bump | `docs: update installation guide` |

### Troubleshooting

#### No Version Bump
**Problem:** Commits don't follow conventional format
**Solution:** Use proper prefixes (`feat:`, `fix:`, etc.)

#### Snapshot Not Created
**Problem:** `build-main.yml` failed
**Solution:** Check build workflow logs, fix issues

#### Release Failed
**Problem:** Missing secrets or permissions
**Solution:** Verify GitHub secrets and variables are configured:

**Repository Secrets:**
- `MC_USERNAME` / `MC_PASSWORD` (Maven Central)
- `GPG_PASSPHRASE` / `GPG_PUBLIC_KEY` / `GPG_SECRET_KEY` (Signing)
- `GPR_READ_TOKEN` / `GPR_WRITE_TOKEN` (GitHub Packages)

**Repository Variables:**
- `GPR_USER` (GitHub Packages username)

#### Unwanted Tags
**Problem:** Extra alpha/marker tags created
**Solution:** Use `release.mode` property consistently

### Testing Releases

#### Local Testing
```bash
# Test snapshot version generation
./gradlew currentVersion -Prelease.mode=snapshot

# Test release version generation
./gradlew currentVersion -Prelease.mode=release

# Run full test suite
./test-release-config.sh
```

#### CI Testing
1. Go to Actions → "Test Release Configuration"
2. Select test scenario (snapshot/release/both)
3. Review test results

### Configuration Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Axion + JReleaser configuration |
| `.github/workflows/release.yml` | Main release workflow |
| `.github/workflows/build-main.yml` | Quality gate for snapshots |
| `.github/workflows/test-release-config.yml` | Release testing |
| `test-release-config.sh` | Local testing script |

### Best Practices

1. **Always test locally** before pushing changes
2. **Use conventional commits** for proper version calculation
3. **Let snapshots happen automatically** - don't manually trigger them usually
4. **Create releases manually** when ready for production
5. **Monitor build-main.yml** - it gates automatic snapshots
6. **Review changelogs** before manual releases