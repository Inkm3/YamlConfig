#!/usr/bin/env bash
set -euo pipefail
root=$(pwd)
# Checkout runs in the Runner helper, not necessarily in this JDK container.
if ! command -v git >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
  apt-get update
  apt-get install -y --no-install-recommends git python3
fi
reference=5cba09ad7237784746b6fe4c0e7f1cf2e8dbcdbf
current=$(git rev-parse HEAD)
if ! git cat-file -e "$reference^{commit}" 2>/dev/null; then
  git fetch --no-tags origin "$reference"
fi
workspace=$(mktemp -d)
trap 'git worktree remove --force "$workspace/reference" >/dev/null 2>&1 || true; rm -rf "$workspace"' EXIT
git worktree add --detach "$workspace/reference" "$reference"
reports="$root/modules/benchmark/build/reports/comparison"
mkdir -p "$reports"
printf 'reference=%s\ncurrent=%s\norder=reference,current,current,reference\n' "$reference" "$current" > "$reports/manifest.txt"
java -version 2> "$reports/java-version.txt"
for pair in 1 2; do
  if [ "$pair" = 1 ]; then order="reference current"; else order="current reference"; fi
  for implementation in $order; do
    if [ "$implementation" = reference ]; then project="$workspace/reference"; else project="$root"; fi
    printf 'JMH_PAIR_START|pair=%s|implementation=%s\n' "$pair" "$implementation"
    bash "$project/gradlew" -p "$project" :benchmark:jmh --no-daemon --stacktrace \
      '-PjmhFilter=.*ConfigBenchmark.moveAndUpdate' -PjmhWarmupIterations=10 -PjmhMeasurementIterations=10
    cp "$project/modules/benchmark/build/reports/jmh/results.json" "$reports/pair-$pair-$implementation.json"
  done
done
python3 "$root/ci/summarize-sequence-jmh.py" "$reports"
tar -czf "$root/modules/benchmark/build/reports/comparison.tar.gz" -C "$root/modules/benchmark/build/reports" comparison
