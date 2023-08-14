import * as core from '@actions/core';

function run() {
    const gradleOutput: string = core.getInput('gradle_output')
    const projectName: string = core.getInput('project_name')
    const koverPrintCoverage = "koverPrintCoverage";

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

    const lineToSearchFor = `> Task :${projectName?.concat(":") ?? ""}${koverPrintCoverage}`
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
    } else {
        console.error("Could not extract root coverage.");
    }
}

run();