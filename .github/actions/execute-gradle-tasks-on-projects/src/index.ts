import * as core from '@actions/core';
import {spawn} from 'child_process';

function run() {
    try {
        const projects = core.getInput('projects');
        const tasks = core.getInput('tasks');
        const parentProjectTask = core.getInput('parent_project_task');

        core.debug(`Projects: ${projects}`);
        core.debug(`Tasks: ${tasks}`);
        core.debug(`Parent Project Task: ${parentProjectTask}`);

        let gradleProjectsTasks: string;
        if (projects === 'buildSrc') {
            core.debug(`only buildSrc has changed, setting gradleProjectsTasks to ${tasks}`);
            gradleProjectsTasks = `${tasks} `;
        } else {
            core.debug(`building gradleProjectsTasks`);
            const projArr: string[] = projects.trim().split(',');
            const taskArr: string[] = tasks.trim().split(',');

            gradleProjectsTasks = projArr.reduce((acc1, proj) => {
                return acc1 + taskArr.reduce((acc2, task) => {
                    return acc2 + `:${proj}:${task} `;
                }, '');
            }, '');
        }
        gradleProjectsTasks += `${parentProjectTask} `;
        const gradleCommand = `./gradlew --stacktrace --console=plain ${gradleProjectsTasks.trim()}`;
        core.debug(`Executing: ${gradleCommand}`);

        const gradleArgs = gradleCommand.split(' ');
        const gradleChild = spawn(gradleArgs[0], gradleArgs.slice(1));

        gradleChild.on('message', (message) => {
            core.debug(message.toString())
        })
        gradleChild.stderr.on('data', (data) => {
            core.error(data.toString())
        })
        gradleChild.on('exit', (code, signal) => {
            if (code !== 0) {
                core.setFailed(`Gradle exited with code ${code} due to signal ${signal}`);
            } else {
                core.setOutput('gradle_output', gradleChild.stdout);
            }
        })

    } catch (error: any) {
        core.setFailed(error.message);
    }
}

run();
