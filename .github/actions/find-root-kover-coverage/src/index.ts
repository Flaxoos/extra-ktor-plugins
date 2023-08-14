import * as core from '@actions/core';

const defaultGreenThreshold = 90;

function run() {
    const gradleOutput: string = core.getInput('gradle_output')
    const projectName: string = core.getInput('project_name')
    const coverageForGreen: string = core.getInput('coverage_for_green')
    const koverPrintCoverage = "koverPrintCoverage";

    const coverageForGreenNumber = coverageForGreen.trim() ? parseFloat(coverageForGreen) : defaultGreenThreshold;
    if (isNaN(Number(coverageForGreen))) {
        core.setFailed("illegal coverage_for_green value: " + coverageForGreen);
    }

    const lines: string[] = gradleOutput.split('\n');
    if (lines.length === 0) {
        core.setFailed("Gradle output does not contain koverPrintCoverage, make sure kover plugin is applied " +
            "and configured to print coverage")
        return;
    }
    if (!gradleOutput.includes(koverPrintCoverage)) {
        core.setFailed(`Gradle output does not contain ${koverPrintCoverage}, make sure kover plugin is applied ` +
            "and configured to print coverage")
        return;
    }

    let koverCoverage: string | undefined;

    core.debug("Project name: " + projectName);
    let projectTaskPrefix = projectName.trim() ? `${projectName}:` : "";
    const lineToSearchFor = `> Task :${projectTaskPrefix}${koverPrintCoverage}`;
    core.debug(`Searching for ${lineToSearchFor}`);
    for (let index = 0; index < lines.length; index++) {
        const line: string = lines[index];
        if (line.includes(lineToSearchFor)) {
            core.debug(`Found ${koverPrintCoverage}`);
            const coverageLine: string = lines[index + 1];
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
        const color = parseFloat(koverCoverage) > parseFloat(coverageForGreen) ? "green" : "yellow";
        core.setOutput('kover_color', color);
    } else {
        core.setFailed(`Kover coverage for ${projectName ?? "root"} could not be found in gradle output.`)
    }
}

run();