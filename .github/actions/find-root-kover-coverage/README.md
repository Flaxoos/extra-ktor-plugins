# Find root kover coverage

This GitHub Action finds the project test coverage based on the Gradle output, assuming the project uses Kover.

## Inputs

### `gradle_output` (required)

Gradle output.

### `project_name` (optional)

The project to find the coverage for. If not provided, it will look for the root project.

## Outputs

### `kover_coverage`

Kover coverage.

## Example usage

```yaml
name: Find root kover coverage
description: Finds the project test coverage based on Gradle output, assuming the project uses Kover

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

    - name: Find root kover coverage
      uses: <YOUR-GITHUB-USERNAME>/<YOUR-GITHUB-REPO>/path/to/action
      with:
        gradle_output: ${{ steps.execute-gradle-tasks.outputs.gradle_output }}
        project_name: 'my-project'
```