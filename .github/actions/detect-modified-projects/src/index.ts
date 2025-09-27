import { execSync } from 'child_process';
import * as core from '@actions/core';

function sh(cmd: string, opts: { silent?: boolean } = {}): string {
    try {
        return execSync(cmd, { encoding: 'utf-8', stdio: opts.silent ? ['ignore', 'pipe', 'pipe'] : 'pipe' }).trim();
    } catch (e) {
        if (!opts.silent) core.debug(`Command failed: ${cmd}\n${String(e)}`);
        throw e;
    }
}

function trySh(cmd: string, opts: { silent?: boolean } = {}): string {
    try {
        return sh(cmd, opts);
    } catch {
        return '';
    }
}

function isShallowRepo(): boolean {
    const out = trySh('git rev-parse --is-shallow-repository', { silent: true });
    return out === 'true';
}

function ensureHistoryAndTags(): void {
    if (isShallowRepo()) {
        core.info('Shallow repo detected → fetching unshallow + tags');
        // If remote is missing (e.g., local testing), this may fail; let it throw to surface the problem.
        sh('git fetch --unshallow --tags --force');
    } else {
        core.info('Complete repo detected → fetching tags/pruning');
        // Keep tags up to date even on full clones
        sh('git fetch --tags --force --prune');
    }
}

/**
 * Determine a sensible base for diff:
 * - On pull_request events: origin/<base_branch>
 * - On push events: HEAD~1 (previous commit on same branch) if exists
 * - Fallback: merge-base with origin/main (adjust default if needed)
 */
function resolveBaseRef(): string {
    const eventName = process.env.GITHUB_EVENT_NAME || '';
    const prBase = process.env.GITHUB_BASE_REF; // set on pull_request events
    if (eventName.startsWith('pull_request') && prBase) {
        core.info(`PR build detected. Using origin/${prBase} as diff base`);
        // Ensure we have the PR base
        trySh(`git fetch origin ${prBase} --force`, { silent: true });
        return `origin/${prBase}`;
    }

    // Push builds: previous commit if available
    const prev = trySh('git rev-parse HEAD~1', { silent: true });
    if (prev) {
        core.info('Push build detected. Using HEAD~1 as diff base');
        return prev;
    }

    // Fallback: merge-base with origin/main (change "main" if your default is different)
    core.info('Fallback diff base: merge-base with origin/main');
    trySh('git fetch origin main --force', { silent: true });
    const mergeBase = trySh('git merge-base HEAD origin/main', { silent: true });
    return mergeBase || 'HEAD';
}

function run(): void {
    try {
        const subprojectPrefixes = (core.getInput('project_prefixes') || '')
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);
        const requiredProjects = (core.getInput('required_projects') || '')
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);

        core.debug('Ensuring full history and tags…');
        ensureHistoryAndTags();

        const githubSha = process.env.GITHUB_SHA;
        if (!githubSha) {
            core.setFailed('GITHUB_SHA not set');
            return;
        }

        const base = resolveBaseRef();
        const diffCmd = `git diff --name-only ${base}..${githubSha}`;

        core.debug(`Executing: ${diffCmd}`);
        core.debug(`Git Status: ${execSync(`git status`, {encoding: 'utf-8'}).trim()}`);
        core.debug(`SHA Exists: ${execSync(`git cat-file -e ${githubSha}`, {encoding: 'utf-8'}).trim()}`);

        let modifiedProjects = execSync(diffCmd, {encoding: 'utf8'});
        core.debug("Modified Projects:" + modifiedProjects)
        core.debug("Required Projects:" + requiredProjects)
        if (modifiedProjects.includes('buildSrc/') && !modifiedProjects.includes('ktor-')) {
            core.debug("only buildSrc has modified");
            modifiedProjects = "buildSrc";
        } else {
            const subprojectPrefixesPattern = subprojectPrefixes.join("|")
            core.debug("subprojectPrefixesPattern: " + subprojectPrefixesPattern);
            const regex = subprojectPrefixes.length > 0
                ? new RegExp(`^(${subprojectPrefixesPattern})`)
                : null;
            let modifiedProjectsArray = modifiedProjects.split('\n')
                .filter(line => {
                    return regex ? regex.test(line) : true;
                })
                .map(line => line.split('/', 1)[0])
                .sort()
                .filter((value, index, self) => self.indexOf(value) === index)
            modifiedProjects = [...new Set(modifiedProjectsArray.concat(requiredProjects))].join(',');
        }
        if (modifiedProjects) {
            core.info(`Modified subprojects including required projects: ${modifiedProjects}`);
            core.setOutput('modified_projects', modifiedProjects);
        } else {
            core.info("No modified subprojects");
        }

    } catch (error) {
        core.setFailed(`Action failed with error: ${error}`);
    }
}

run();
