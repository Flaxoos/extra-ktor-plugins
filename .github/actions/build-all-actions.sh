#!/bin/bash

for dir in ./*/ ; do
    if [ -f "${dir}package.json" ]; then
        echo "Running npm run build in ${dir}"
        cd "$dir" || continue
        npm run build
        cd - || continue
    fi
done
