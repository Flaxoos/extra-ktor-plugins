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
function sh(cmd) {
    return (0, child_process_1.execSync)(cmd, { encoding: 'utf-8' }).trim();
}
function trySh(cmd) {
    try {
        return sh(cmd);
    }
    catch {
        return '';
    }
}
/**
 * Determine a sensible base for diff:
 * - On pull_request events: origin/<base_branch>
 * - On push events: HEAD~1 (previous commit on same branch) if it exists
 * - Fallback: merge-base with origin/main (adjust default branch name if needed)
 */
function resolveBaseRef() {
    const eventName = process.env.GITHUB_EVENT_NAME || '';
    const prBase = process.env.GITHUB_BASE_REF; // set on pull_request events
    if (eventName.startsWith('pull_request') && prBase) {
        // Ensure we have the PR base locally
        trySh(`git fetch origin ${prBase} --force`);
        return `origin/${prBase}`;
    }
    // Push builds: previous commit if available
    const prev = trySh('git rev-parse HEAD~1');
    if (prev)
        return prev;
    // Fallback: merge-base with origin/main (change "main" if your default is different)
    trySh('git fetch origin main --force');
    const mergeBase = trySh('git merge-base HEAD origin/main');
    return mergeBase || 'HEAD';
}
function run() {
    var _a;
    try {
        const subprojectPrefixes = (core.getInput('project_prefixes') || '')
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);
        const requiredProjects = (core.getInput('required_projects') || '')
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);
        // We assume Actions checkout used fetch-depth: 0 and fetch-tags: true.
        // Only refresh tags/prune; do NOT attempt to unshallow here.
        trySh('git fetch --tags --force --prune');
        const githubSha = process.env.GITHUB_SHA;
        if (!githubSha) {
            core.setFailed('GITHUB_SHA not set');
            return;
        }
        const base = resolveBaseRef();
        const diffCmd = `git diff --name-only ${base}..${githubSha}`;
        core.debug(`Executing: ${diffCmd}`);
        core.debug(`Git Status:\n${trySh('git status')}`);
        const changed = trySh(diffCmd);
        core.debug('Changed files:\n' + (changed || '(none)'));
        core.debug('Required Projects: ' + JSON.stringify(requiredProjects));
        let modifiedProjects = '';
        if (changed.includes('buildSrc/') && !changed.includes('ktor-')) {
            core.debug('Only buildSrc modified → limiting to buildSrc');
            modifiedProjects = 'buildSrc';
        }
        else {
            const pattern = subprojectPrefixes.join('|');
            core.debug('subprojectPrefixesPattern: ' + pattern);
            const regex = subprojectPrefixes.length > 0 ? new RegExp(`^(${pattern})`) : null;
            const modifiedProjectsArray = changed
                .split('\n')
                .filter(Boolean)
                .filter(line => (regex ? regex.test(line) : true))
                .map(line => line.split('/', 1)[0]) // project dir
                .filter(Boolean)
                .sort()
                .filter((v, i, a) => a.indexOf(v) === i); // uniq
            const merged = Array.from(new Set([...modifiedProjectsArray, ...requiredProjects]));
            modifiedProjects = merged.join(',');
        }
        if (modifiedProjects) {
            core.info(`Modified subprojects (including required): ${modifiedProjects}`);
            core.setOutput('modified_projects', modifiedProjects);
        }
        else {
            core.info('No modified subprojects');
            core.setOutput('modified_projects', '');
        }
    }
    catch (error) {
        core.setFailed(`Action failed with error: ${(_a = error === null || error === void 0 ? void 0 : error.message) !== null && _a !== void 0 ? _a : String(error)}`);
    }
}
run();
