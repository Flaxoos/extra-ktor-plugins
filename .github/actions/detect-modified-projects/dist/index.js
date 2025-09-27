"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const child_process_1 = require("child_process");
const core = __importStar(require("@actions/core"));
function sh(cmd, opts = {}) {
    try {
        return (0, child_process_1.execSync)(cmd, { encoding: 'utf-8', stdio: opts.silent ? ['ignore', 'pipe', 'pipe'] : 'pipe' }).trim();
    }
    catch (e) {
        if (!opts.silent)
            core.debug(`Command failed: ${cmd}\n${String(e)}`);
        throw e;
    }
}
function trySh(cmd, opts = {}) {
    try {
        return sh(cmd, opts);
    }
    catch {
        return '';
    }
}
function isShallowRepo() {
    const out = trySh('git rev-parse --is-shallow-repository', { silent: true });
    return out === 'true';
}
function ensureHistoryAndTags() {
    // Detect shallow/non-shallow
    let isShallow;
    try {
        const out = (0, child_process_1.execSync)('git rev-parse --is-shallow-repository', { encoding: 'utf-8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
        isShallow = out === 'true';
    }
    catch {
        // If the command isn't supported (very old git), assume not shallow
        isShallow = false;
    }
    core.info(`Repo shallow: ${isShallow}`);
    if (isShallow) {
        // Shallow checkout → unshallow and fetch tags
        core.info('Fetching to unshallow repository and include tags…');
        (0, child_process_1.execSync)('git fetch --unshallow --tags --force', { encoding: 'utf-8' });
    }
    else {
        // Already complete → just refresh tags and prune
        core.info('Repository already complete; fetching tags and pruning…');
        (0, child_process_1.execSync)('git fetch --tags --force --prune', { encoding: 'utf-8' });
    }
}
/**
 * Determine a sensible base for diff:
 * - On pull_request events: origin/<base_branch>
 * - On push events: HEAD~1 (previous commit on same branch) if exists
 * - Fallback: merge-base with origin/main (adjust default if needed)
 */
function resolveBaseRef() {
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
function run() {
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
        core.debug(`Git Status: ${(0, child_process_1.execSync)(`git status`, { encoding: 'utf-8' }).trim()}`);
        core.debug(`SHA Exists: ${(0, child_process_1.execSync)(`git cat-file -e ${githubSha}`, { encoding: 'utf-8' }).trim()}`);
        let modifiedProjects = (0, child_process_1.execSync)(diffCmd, { encoding: 'utf8' });
        core.debug("Modified Projects:" + modifiedProjects);
        core.debug("Required Projects:" + requiredProjects);
        if (modifiedProjects.includes('buildSrc/') && !modifiedProjects.includes('ktor-')) {
            core.debug("only buildSrc has modified");
            modifiedProjects = "buildSrc";
        }
        else {
            const subprojectPrefixesPattern = subprojectPrefixes.join("|");
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
                .filter((value, index, self) => self.indexOf(value) === index);
            modifiedProjects = [...new Set(modifiedProjectsArray.concat(requiredProjects))].join(',');
        }
        if (modifiedProjects) {
            core.info(`Modified subprojects including required projects: ${modifiedProjects}`);
            core.setOutput('modified_projects', modifiedProjects);
        }
        else {
            core.info("No modified subprojects");
        }
    }
    catch (error) {
        core.setFailed(`Action failed with error: ${error}`);
    }
}
run();
