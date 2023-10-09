import {execSync} from 'child_process';
import * as core from '@actions/core';

function run() {
    try {
        const subprojectPrefixes = core.getInput('project_prefixes')?.split(",") ?? [];
        const requiredProjects = core.getInput('required_projects')?.split(",") ?? [];

        core.debug("executing git fetch");
        execSync('git fetch --unshallow', {encoding: 'utf-8'});

        const githubSha = process.env.GITHUB_SHA;
        if (!githubSha) {
            core.setFailed('GITHUB_SHA not set')
        }
        const diffCmd = `git diff --name-only HEAD~1..${githubSha}`;

        core.debug(`Executing: ${diffCmd}`);
        core.debug(`Git Status: ${execSync(`git status`, {encoding: 'utf-8'}).trim()}`);
        core.debug(`SHA Exists: ${execSync(`git cat-file -e ${githubSha}`, {encoding: 'utf-8'}).trim()}`);

        let modifiedProjects = execSync(diffCmd, {encoding: 'utf8'});
        core.debug("Modified Projects:" + modifiedProjects)
        core.debug("Required Projects:" + requiredProjects)
        if (modifiedProjects.includes('buildSrc/') && !modifiedProjects.includes('ktor-')) {
            core.debug("only buildSrc has modified");
            modifiedProjects = "buildSrc";
        } else {
            const subprojectPrefixesPattern = subprojectPrefixes.join("|")
            core.debug("subprojectPrefixesPattern: " + subprojectPrefixesPattern);
            const regex = subprojectPrefixes.length > 0
                ? new RegExp(`^(${subprojectPrefixesPattern})`)
                : null;
            let modifiedProjectsArray = modifiedProjects.split('\n')
                .filter(line => {
                    return regex ? regex.test(line) : true;
                })
                .map(line => line.split('/', 1)[0])
                .sort()
                .filter((value, index, self) => self.indexOf(value) === index)
            modifiedProjects = [...new Set(modifiedProjectsArray.concat(requiredProjects))].join(',');
        }
        if (modifiedProjects) {
            core.info(`Modified subprojects including required projects: ${modifiedProjects}`);
            core.setOutput('modified_projects', modifiedProjects);
        } else {
            core.info("No modified subprojects");
        }

    } catch (error) {
        core.setFailed(`Action failed with error: ${error}`);
    }
}

run();
