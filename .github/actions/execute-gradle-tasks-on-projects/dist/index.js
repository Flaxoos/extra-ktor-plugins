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
const child_process_1 = require("child_process");
function run() {
    try {
        const projects = core.getInput('projects');
        const tasks = core.getInput('tasks');
        const parentProjectTask = core.getInput('parent_project_task');
        core.debug(`Projects: ${projects}`);
        core.debug(`Tasks: ${tasks}`);
        core.debug(`Parent Project Task: ${parentProjectTask}`);
        let gradleProjectsTasks;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks}`);
            gradleProjectsTasks = `${tasks} `;
        }
        else {
            core.debug(`building gradleProjectsTasks`);
            const projArr = projects.trim().split(',');
            const taskArr = tasks.trim().split(',');
            gradleProjectsTasks = projArr.reduce((acc1, proj) => {
                return acc1 + taskArr.reduce((acc2, task) => {
                    return acc2 + `:${proj}:${task} `;
                }, '');
            }, '');
        }
        gradleProjectsTasks += `${parentProjectTask} `;
        const gradleCommand = `./gradlew --info --stacktrace --console=plain ${gradleProjectsTasks.trim()}`;
        core.info(`Executing: ${gradleCommand}`);
        const gradleArgs = gradleCommand.split(' ');
        const gradleChild = (0, child_process_1.spawn)(gradleArgs[0], gradleArgs.slice(1));
        gradleChild.stdout.on('data', (data) => {
            core.info(data.toString());
        });
        gradleChild.stderr.on('data', (data) => {
            core.error(data.toString());
        });
        gradleChild.on('exit', (code, signal) => {
            if (code !== 0) {
                core.setFailed(`Gradle exited with code ${code} due to signal ${signal}`);
            }
            else {
                core.setOutput('gradle_output', gradleChild.stdout);
            }
        });
    }
    catch (error) {
        core.setFailed(error.message);
    }
}
run();
