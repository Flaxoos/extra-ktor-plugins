import * as core from '@actions/core';
import {spawn} from 'child_process';

async function run() {
    try {
        let projects = core.getInput('projects', {trimWhitespace: true});
        let tasks = core.getInput('tasks', {trimWhitespace: true});
        let executeOnRootAnyway = core.getBooleanInput('execute_on_root_anyway')
        let parentProjectTask = core.getInput('parent_project_task', {trimWhitespace: true});

        core.debug(`Projects: ${projects ?? "--EMPTY INPUT AFTER TRIMMING--"}`);
        core.debug(`Tasks: ${tasks ?? "--EMPTY INPUT AFTER TRIMMING--"}`);
        core.debug(`Parent Project Task: ${parentProjectTask ?? "--EMPTY INPUT AFTER TRIMMING--"}`);

        const taskArr: string[] = tasks.split(',').filter((p) => p.trim() !== '');
        core.debug("Task array: " + taskArr);
        let gradleProjectsTasks: string;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks}`);
            gradleProjectsTasks = `${tasks} `;
        } else {
            const projArr: string[] = projects.split(',').filter((p) => p.trim() !== '');
            core.debug(`building gradleProjectsTasks with projects: ${projArr} and tasks: ${taskArr}`);
            if (taskArr.length === 0 && !parentProjectTask) {
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
        gradleProjectsTasks += parentProjectTask ? `${parentProjectTask} ` : ``;
        const gradleCommand = `./gradlew --stacktrace ${gradleProjectsTasks.trim()}`;
        core.info(`Executing: ${gradleCommand}`);
        const gradleArgs = gradleCommand.split(' ');
        const gradleChild = spawn(gradleArgs[0], gradleArgs.slice(1));

        const processPromise = new Promise<void>((resolve, reject) => {
            gradleChild.stdout.on('data', (data) => {
                core.info(data.toString());
            });

            gradleChild.stderr.on('data', (data) => {
                core.error(data.toString());
            });

            gradleChild.on('exit', (code, signal) => {
                if (code !== 0) {
                    reject(new Error(`Gradle exited with code ${code} due to signal ${signal}`));
                } else {
                    core.setOutput('gradle_output', gradleChild.stdout);
                    resolve();
                }
            });
        });

        await processPromise;

    } catch (error: any) {
        core.setFailed(error.message);
    }
}

run();
