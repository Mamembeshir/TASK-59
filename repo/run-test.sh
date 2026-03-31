#!/usr/bin/env bash

set -euo pipefail

echo "[run-test.sh] Starting integration test suite..."
./mvnw -B verify -Pintegration-tests
echo "[run-test.sh] Integration test suite completed successfully."
