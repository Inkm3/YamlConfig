# JVM compatibility tests

Production Kotlin and Java compilation uses Java 17. `testJavaVersion` changes
Test launchers only:

```shell
bash ./gradlew build -PtestJavaVersion=17
bash ./gradlew build -PtestJavaVersion=21
bash ./gradlew build -PtestJavaVersion=25
```

GitLab CI uses three matrix jobs; the GitHub workflow mirrors them after the
owner synchronizes the completed work. All execute build, not fewer tests.
Gradle runs on Java 17; toolchains locate an installed matching JVM or provision
one through Foojay. First builds therefore need dependency/JDK download access.
No credentials or private JDK download URL is embedded in source.

Java tests check Runtime.version().feature() against the requested version and
representative published classfile major version 61 (Java 17). These are not a
complete binary compatibility proof. IDE runners should delegate to Gradle or
supply yamlconfig.expectedTestJavaVersion. The ordinary CLI default is 17.
Report cache hits as reuse, not new execution. Use --rerun-tasks --no-build-cache
when deliberately requiring a fresh verification.

GitLab duplicate branch-pipeline suppression only applies to push events with
an existing MR. An API/manual verification pipeline is not suppressed merely
because a branch has a merge request. Success must be checked on the uploaded SHA.

References:
- https://docs.gradle.org/current/userguide/toolchains.html
- https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention/1.0.0
- https://docs.gitlab.com/ci/yaml/workflow/
