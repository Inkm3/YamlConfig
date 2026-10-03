#!/usr/bin/env bash
set -euo pipefail
root=$(pwd)
version=$(sed -n 's/^projectVersion=//p' gradle.properties | tr -d '\r')
if [ -z "$version" ]; then echo 'Missing projectVersion' >&2; exit 1; fi
# Isolate the repository; never accidentally consume stale mavenLocal artifacts.
rm -rf build/smoke-repository
bash ./gradlew :core:publishMavenPublicationToSmokeRepository :snakeyaml:publishMavenPublicationToSmokeRepository --stacktrace --no-daemon
for module in core snakeyaml; do
  directory="build/smoke-repository/com/github/inkm3/yamlconfig/$module/$version"
  test -d "$directory"
  test -n "$(find "$directory" -name '*.pom' -print -quit)"
  test -n "$(find "$directory" -name '*-sources.jar' -print -quit)"
done
bash ./gradlew -p samples/java-consumer clean run --stacktrace --no-daemon \
  -PsmokeRepository="$root/build/smoke-repository" -PlibraryVersion="$version"
printf 'PUBLISHED_MODULES_OK: %s\n' "$version"
