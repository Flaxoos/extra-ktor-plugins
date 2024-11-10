"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function (o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = {
            enumerable: true, get: function () {
                return m[k];
            }
        };
    }
    Object.defineProperty(o, k2, desc);
}) : (function (o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function (o, v) {
    Object.defineProperty(o, "default", {enumerable: true, value: v});
}) : function (o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", {value: true});
const core = __importStar(require("@actions/core"));
const child_process_1 = require("child_process");

class StringBuilder {
    constructor() {
        this._parts = [];
    }

    append(value) {
        this._parts.push(value);
    }

    toString() {
        return this._parts.join("");
    }
}

async function run() {
    var _a, _b;
    try {
        let projects = core.getInput('projects');
        let tasks = core.getInput('tasks');
        let executeOnRootAnyway = (_b = ((_a = core.getInput('execute_on_root_anyway', {
            trimWhitespace: true,
        })) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === 'true') !== null && _b !== void 0 ? _b : false;
        let rootProjectTask = core.getInput('parent_project_task', {
            required: false
        });
        core.debug(`Projects: '${projects}'`);
        core.debug(`Tasks: '${tasks}'`);
        core.debug(`Root project Task: '${rootProjectTask}'`);
        const taskArr = tasks.split(',').filter((p) => p.trim() !== '');
        core.debug("Task array: " + taskArr);
        let gradleProjectsTasks;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks.replace(",", " ")}`);
            gradleProjectsTasks = `${tasks.replace(",", " ")} `;
        } else {
            const projArr = projects.split(',').filter((p) => p.trim() !== '');
            core.debug(`building gradleProjectsTasks with projects: ${projArr} and tasks: ${taskArr}`);
            if (taskArr.length === 0 && !rootProjectTask) {
                core.info("No tasks provided, skipping");
                return;
            }
            if (projArr.length === 0 && !executeOnRootAnyway) {
                core.info("No projects to build, skipping");
                return;
            }
            if (projArr.length > 0) {
                gradleProjectsTasks = projArr.reduce((acc1, proj) => {
                    return acc1 + taskArr.reduce((acc2, task) => {
                        return acc2 + `:${proj}:${task} `;
                    }, '');
                }, '');
            } else {
                gradleProjectsTasks = taskArr.reduce((acc1, task) => {
                    return acc1 + `${task} `;
                }, '');
            }
        }
        if (rootProjectTask) {
            core.debug(`Adding root project task: ${rootProjectTask} to command`);
            gradleProjectsTasks += rootProjectTask;
        }
        const gradleCommand = `./gradlew --stacktrace ${gradleProjectsTasks.trim()}`;
        core.info(`Executing: ${gradleCommand}`);
        const gradleArgs = gradleCommand.split(' ');
        const gradleChild = (0, child_process_1.spawn)(gradleArgs[0], gradleArgs.slice(1));
        const processPromise = new Promise((resolve, reject) => {
            let gradleOutputBuilder = new StringBuilder();
            gradleChild.stdout.on('data', (data) => {
                gradleOutputBuilder.append(data.toString());
                core.info(data.toString());
            });
            gradleChild.stderr.on('data', (data) => {
                core.error(data.toString());
            });
            gradleChild.on('exit', (code, signal) => {
                if (code !== 0) {
                    reject(new Error(`Gradle exited with code ${code} due to signal ${signal}`));
                } else {
                    resolve(gradleOutputBuilder.toString());
                }
            });
        });
        core.setOutput('gradle_output', await processPromise);
    } catch (error) {
        core.setFailed(error.message);
    }
}

run();
