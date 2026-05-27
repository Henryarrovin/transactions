#!/bin/bash

# Load .env if present
if [ -f .env ]; then
    # shellcheck disable=SC2046
    export $(grep -v '^#' .env | xargs)
fi

echo "Environment variables loaded."

cd ..

./gradlew bootRun