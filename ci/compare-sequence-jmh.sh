#!/usr/bin/env bash
set -euo pipefail
root=$(pwd)
# Checkout happens in the Runner helper container; the JDK image need not contain Git.
if ! command -v git >/dev/null 2>&1; then
  apt-get update
  apt-get install -y --no-install-recommends git
fi
reference=5cba09ad7237784746b6fe4c0e7f1cf2e8dbcdbf
if ! git cat-file -e "$reference^{commit}"; then
  git fetch --no-tags origin "$reference"
fi
workspace=$(mktemp -d)
trap 'git worktree remove --force "$workspace/reference" >/dev/null 2>&1 || true; rm -rf "$workspace"' EXIT
git worktree add --detach "$workspace/reference" "$reference"
mkdir -p "$root/modules/benchmark/build/reports/comparison"
for implementation in reference current; do
  if [ "$implementation" = reference ]; then project="$workspace/reference"; else project="$root"; fi
  bash "$project/gradlew" -p "$project" :benchmark:jmh --no-daemon --stacktrace \
    '-PjmhFilter=.*ConfigBenchmark.moveAndUpdate' -PjmhWarmupIterations=10 -PjmhMeasurementIterations=10
  cp "$project/modules/benchmark/build/reports/jmh/results.json" \
    "$root/modules/benchmark/build/reports/comparison/$implementation.json"
done
printf 'JMH_COMPARISON_BASE64_BEGIN\n'
tar -czf "$root/modules/benchmark/build/reports/comparison.tar.gz" -C "$root/modules/benchmark/build/reports" comparison
base64 -w 0 "$root/modules/benchmark/build/reports/comparison.tar.gz"
printf '\nJMH_COMPARISON_BASE64_END\n'
