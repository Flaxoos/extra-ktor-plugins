import {execSync} from 'child_process';
import * as core from '@actions/core';

function run() {
    try {
        const subprojectPrefixes = core.getInput('project_prefixes')?.split(",") ?? [];

        core.debug("executing git fetch");
        execSync('git fetch');

        const githubSha = process.env.GITHUB_SHA;
        if (!githubSha) {
            core.setFailed('GITHUB_SHA not set')
        }
        const diffCmd = `git diff --name-only origin/main..${githubSha}`
        core.debug("Calling: " + diffCmd);
        let modifiedProjects = execSync(diffCmd, {encoding: 'utf8'});
        core.debug("Result:" + modifiedProjects)
        if (modifiedProjects.includes('buildSrc/') && !modifiedProjects.includes('ktor-')) {
            core.debug("only buildSrc has modified");
            modifiedProjects = "buildSrc";
        } else {
            const subprojectPrefixesPattern = subprojectPrefixes.join("|")
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
        } else {
            core.info("No modified subprojects");
        }

    } catch (error) {
        core.setFailed(`Action failed with error: ${error}`);
    }
}

run();
