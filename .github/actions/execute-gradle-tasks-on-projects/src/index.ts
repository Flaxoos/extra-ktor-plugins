import * as core from '@actions/core';
import {spawn} from 'child_process';

async function run() {
    try {
        let projects = core.getInput('projects');
        let tasks = core.getInput('tasks');
        const parentProjectTask = core.getInput('parent_project_task');

        projects = projects.trim().replace("\n", " ");
        tasks = tasks.trim().replace("\n", " ");

        core.debug(`Projects: ${projects??"--EMPTY INPUT--"}`);
        core.debug(`Tasks: ${tasks??"--EMPTY INPUT--"}`);
        core.debug(`Parent Project Task: ${parentProjectTask??"--EMPTY INPUT--"}`);

        let gradleProjectsTasks: string;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks}`);
            gradleProjectsTasks = `${tasks} `;
        } else {
            core.debug(`building gradleProjectsTasks`);
            const projArr: string[] = projects.split(',');
            const taskArr: string[] = tasks.split(',');
            if (projArr.length == 0) {
                core.info("No projects to build, skipping");
                return;
            }

            gradleProjectsTasks = projArr.reduce((acc1, proj) => {
                return acc1 + taskArr.reduce((acc2, task) => {
                    return acc2 + `:${proj}:${task} `;
                }, '');
            }, '');
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
