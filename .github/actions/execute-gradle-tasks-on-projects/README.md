## Execute gradle tasks on projects

This GitHub Action allows you to run gradle tasks on projects within your repository.

### Inputs

-  `projects`  (required): Projects to run the tasks on, comma-separated.
-  `tasks`  (required): Tasks to run on the projects, comma-separated.
-  `parent_project_task`  (optional): Task to run on the parent project.
-  `execute_on_root_anyway`  (optional): Execute on root regardless of if any projects have been provided. Accepted values: 'true' or 'false'.

### Outputs

-  `gradle_output` : Output from Gradle.

### Example Usage
```yaml
name: Execute gradle tasks on projects
description: Run gradle tasks on projects

on:
  push:
    branches:
      - main

jobs:
  build:
  runs-on: ubuntu-latest
  steps:
    - name: Checkout repository
      uses: actions/checkout@v2

    - name: Execute gradle tasks
      uses: <YOUR-GITHUB-USERNAME>/<YOUR-GITHUB-REPO>/path/to/action
      with:
        projects: 'project1,project2'
        tasks: 'build,test'
        parent_project_task: 'clean'
        execute_on_root_anyway: 'false'
```