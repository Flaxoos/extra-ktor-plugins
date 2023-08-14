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
async function run() {
    try {
        let projects = core.getInput('projects');
        let tasks = core.getInput('tasks');
        let executeOnRootAnyway = core.getInput('execute_on_root_anyway').toLowerCase() === 'true';
        let parentProjectTask = core.getInput('parent_project_task');
        projects = projects.trim().replace("\n", " ");
        tasks = tasks.trim().replace("\n", " ");
        parentProjectTask = parentProjectTask.trim().replace("\n", " ");
        core.debug(`Projects: ${projects !== null && projects !== void 0 ? projects : "--EMPTY INPUT AFTER TRIMMING--"}`);
        core.debug(`Tasks: ${tasks !== null && tasks !== void 0 ? tasks : "--EMPTY INPUT AFTER TRIMMING--"}`);
        core.debug(`Parent Project Task: ${parentProjectTask !== null && parentProjectTask !== void 0 ? parentProjectTask : "--EMPTY INPUT AFTER TRIMMING--"}`);
        const taskArr = tasks.split(',');
        core.debug("Task array: " + taskArr);
        let gradleProjectsTasks;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks}`);
            gradleProjectsTasks = `${tasks} `;
        }
        else {
            const projArr = projects.split(',');
            core.debug(`building gradleProjectsTasks with projects: ${projArr} and tasks: ${taskArr}`);
            if (taskArr.length === 0 && !parentProjectTask) {
                core.info("No tasks provided, skipping");
                return;
            }
            if (projArr.length == 0 && !executeOnRootAnyway) {
                core.info("No projects to build, skipping");
                return;
            }
            if (projArr.length > 0) {
                gradleProjectsTasks = projArr.reduce((acc1, proj) => {
                    return acc1 + taskArr.reduce((acc2, task) => {
                        return acc2 + `:${proj}:${task} `;
                    }, '');
                }, '');
            }
            else {
                gradleProjectsTasks = taskArr.reduce((acc1, task) => {
                    return acc1 + `${task} `;
                }, '');
            }
        }
        gradleProjectsTasks += parentProjectTask ? `${parentProjectTask} ` : ``;
        const gradleCommand = `./gradlew --stacktrace ${gradleProjectsTasks.trim()}`;
        core.info(`Executing: ${gradleCommand}`);
        const gradleArgs = gradleCommand.split(' ');
        const gradleChild = (0, child_process_1.spawn)(gradleArgs[0], gradleArgs.slice(1));
        const processPromise = new Promise((resolve, reject) => {
            gradleChild.stdout.on('data', (data) => {
                core.info(data.toString());
            });
            gradleChild.stderr.on('data', (data) => {
                core.error(data.toString());
            });
            gradleChild.on('exit', (code, signal) => {
                if (code !== 0) {
                    reject(new Error(`Gradle exited with code ${code} due to signal ${signal}`));
                }
                else {
                    core.setOutput('gradle_output', gradleChild.stdout);
                    resolve();
                }
            });
        });
        await processPromise;
    }
    catch (error) {
        core.setFailed(error.message);
    }
}
run();
