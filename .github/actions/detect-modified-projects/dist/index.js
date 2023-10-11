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
function run() {
    var _a, _b;
    try {
        const subprojectPrefixes = (_b = (_a = core.getInput('project_prefixes')) === null || _a === void 0 ? void 0 : _a.split(",")) !== null && _b !== void 0 ? _b : [];
        core.debug("executing git fetch");
        (0, child_process_1.execSync)('git fetch --unshallow', { encoding: 'utf-8' });
        const githubSha = process.env.GITHUB_SHA;
        if (!githubSha) {
            core.setFailed('GITHUB_SHA not set');
        }
        const diffCmd = `git diff --name-only HEAD~1..${githubSha}`;
        core.debug(`Executing: ${diffCmd}`);
        core.debug(`Git Status: ${(0, child_process_1.execSync)(`git status`, { encoding: 'utf-8' }).trim()}`);
        core.debug(`SHA Exists: ${(0, child_process_1.execSync)(`git cat-file -e ${githubSha}`, { encoding: 'utf-8' }).trim()}`);
        let modifiedProjects = (0, child_process_1.execSync)(diffCmd, { encoding: 'utf8' });
        core.debug("Result:" + modifiedProjects);
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
            modifiedProjects = modifiedProjects.split('\n')
                .filter(line => {
                return regex ? regex.test(line) : true;
            })
                .map(line => line.split('/', 1)[0])
                .sort()
                .filter((value, index, self) => self.indexOf(value) === index)
                .join(',');
        }
        if (modifiedProjects) {
            core.info(`Modified subprojects: ${modifiedProjects}`);
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
