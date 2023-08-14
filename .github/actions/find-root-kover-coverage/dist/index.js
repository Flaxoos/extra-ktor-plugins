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
const core = __importStar(require("@actions/core"));
function run() {
    const gradleOutput = core.getInput('gradle_output');
    const projectName = core.getInput('project_name');
    const koverPrintCoverage = "koverPrintCoverage";
    const lines = gradleOutput.split('\n');
    if (lines.length === 0) {
        core.setFailed("Gradle output does not contain koverPrintCoverage, make sure kover plugin is applied " +
            "and configured to print coverage");
        return;
    }
    if (!gradleOutput.includes(koverPrintCoverage)) {
        core.setFailed(`Gradle output does not contain ${koverPrintCoverage}, make sure kover plugin is applied ` +
            "and configured to print coverage");
        return;
    }
    let koverCoverage;
    core.debug("Project name: " + projectName);
    let projectTaskPrefix = projectName.trim() ? `${projectName}:` : "";
    const lineToSearchFor = `> Task :${projectTaskPrefix}${koverPrintCoverage}`;
    core.debug(`Searching for ${lineToSearchFor}`);
    for (let index = 0; index < lines.length; index++) {
        const line = lines[index];
        if (line.includes(lineToSearchFor)) {
            core.debug(`Found ${koverPrintCoverage}`);
            const coverageLine = lines[index + 1];
            const match = coverageLine.split('application line coverage: ');
            if (match && match[1]) {
                koverCoverage = match[1].replace("%", "");
                break;
            }
        }
    }
    if (koverCoverage) {
        core.debug("kover_coverage: " + koverCoverage);
        core.setOutput('kover_coverage', koverCoverage);
    }
    else {
        core.setFailed(`Kover coverage for ${projectName !== null && projectName !== void 0 ? projectName : "root"} could not be found in gradle output.`);
    }
}
run();
