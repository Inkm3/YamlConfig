#!/usr/bin/env bash
set -euo pipefail
# Confirm the normal check succeeds first, then prove it rejects a real missing API.
bash ./gradlew :core:checkKotlinAbi :snakeyaml:checkKotlinAbi --stacktrace --no-daemon
reference=modules/core/api/core.api
saved=$(mktemp)
cp "$reference" "$saved"
trap 'cp "$saved" "$reference"; rm -f "$saved"' EXIT
mkdir -p build/reports/abi-guard
sed '/public final fun load ()Lcom\/github\/inkm3\/yamlconfig\/YamlConfigSession;/d' "$saved" > "$reference"
if cmp -s "$saved" "$reference"; then
  echo 'Expected load signature not found; guard probe is invalid.' >&2
  exit 1
fi
set +e
bash ./gradlew :core:checkKotlinAbi --no-daemon > build/reports/abi-guard/rejected.log 2>&1
result=$?
set -e
cat build/reports/abi-guard/rejected.log
if [ "$result" -eq 0 ]; then
  echo 'ABI check unexpectedly accepted a removed reference signature.' >&2
  exit 1
fi
if ! grep -q 'load ()Lcom/github/inkm3/yamlconfig/YamlConfigSession;' build/reports/abi-guard/rejected.log; then
  echo 'Failure did not report the intended ABI difference.' >&2
  exit 1
fi
cp "$saved" "$reference"
bash ./gradlew :core:checkKotlinAbi --no-daemon
printf 'ABI_GUARD_OK: changed signature rejected; original restored and accepted.\n'
