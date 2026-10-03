# JVM compatibility tests

Production Kotlin and Java compilation uses Java 17. `testJavaVersion` changes
Test launchers only:

```shell
bash ./gradlew build -PtestJavaVersion=17
bash ./gradlew build -PtestJavaVersion=21
bash ./gradlew build -PtestJavaVersion=25
```

GitLab CI uses three matrix jobs; the GitHub workflow mirrors them after the
owner synchronizes the completed work. All execute build, including ABI checks.
Gradle runs on Java 17; toolchains locate an installed matching JVM or provision
one through Foojay. First builds need dependency/JDK download access.

Java tests check Runtime.version().feature() against the requested version and
representative published classfile major version 61 (Java 17). These are not a
complete binary compatibility proof. IDE runners should delegate to Gradle or
supply yamlconfig.expectedTestJavaVersion. The ordinary CLI default is 17.

Both CI configurations also run an ABI negative probe and standalone Java/Kotlin
published-artifact consumers after the matrix succeeds. A deliberately failing
ABI check appears in the probe log; the job succeeds only when that exact
mismatch was rejected and the restored baseline passes.

For release verification, create a GitLab pipeline with `FRESH_VERIFY=true`.
All three build jobs then pass `--rerun-tasks --no-build-cache`, so compile/test
results are newly executed. Download/dependency caches can still be used; this is
not a claim of a network-cold build. Normal jobs keep their useful cache behavior.

`ABI_BOOTSTRAP=true` is maintenance-only and is NOT full verification. Never merge
merely because the bootstrap pipeline is green. Check each verify job and quality
job on the exact source SHA, then check the integration pipeline after merging.

Tag pushes are covered by both CI configurations. No automatic external release,
GitHub visibility change, JitPack build or tag creation is performed by these jobs.

References:
- https://docs.gradle.org/current/userguide/toolchains.html
- https://docs.gitlab.com/ci/yaml/workflow/
