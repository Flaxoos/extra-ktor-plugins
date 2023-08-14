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
    var _a;
    const gradleOutput = core.getInput('gradle_output');
    const projectName = core.getInput('project_name');
    if (!gradleOutput.includes("koverPrintCoverage")) {
        core.error("Gradle output does not contain koverPrintCoverage, make sure kover plugin is applied " +
            "and configured to print coverage");
    }
    const lines = gradleOutput.split('\n');
    if (lines.length === 0) {
        core.error("Empty gradle output provided.");
        return;
    }
    let koverCoverage;
    for (let index = 0; index < lines.length; index++) {
        const line = lines[index];
        if (line.includes(`> Task :${(_a = projectName === null || projectName === void 0 ? void 0 : projectName.concat(":")) !== null && _a !== void 0 ? _a : ""}koverPrintCoverage`)) {
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
        console.error("Could not extract root coverage.");
    }
}
run();
