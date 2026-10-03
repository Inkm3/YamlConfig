#!/usr/bin/env bash
set -euo pipefail
# Explicit maintenance command only. Ordinary CI never regenerates the baseline.
bash ./gradlew :core:updateKotlinAbi :snakeyaml:updateKotlinAbi --stacktrace --no-daemon
mkdir -p build
find modules/core/api modules/snakeyaml/api -type f -print
tar -czf build/abi-reference.tar.gz modules/core/api modules/snakeyaml/api
printf '\nABI_REFERENCE_BASE64_BEGIN\n'
base64 -w 0 build/abi-reference.tar.gz
printf '\nABI_REFERENCE_BASE64_END\n'
