#!/bin/bash
IFS=$'\n' read -rd '' -a lines <<<"$GRADLE_OUTPUT"
for index in "${!lines[@]}"; do
  line=${lines[index]}
  if [[ $line == "Task :koverPrintCoverage" ]]; then
    COVERAGE_LINE=${lines[index+1]}
    break
  fi
done
ROOT_COVERAGE=$(echo "$COVERAGE_LINE" | awk -F 'application line coverage: ' '{print $2}')
echo $ROOT_COVERAGE
