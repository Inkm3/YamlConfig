# Forked JMH benchmarks

`modules/benchmark` is a non-published subproject. It uses JMH 1.37 with the Java
annotation processor; merely adding jmh-core is not sufficient. Kotlin fixtures
exercise the real public API and Java benchmark methods provide generated harnesses.
No production runtime dependency or proprietary annotation is added.

```shell
bash ./gradlew :benchmark:jmh
```

The default run uses two forked Java 17 JVMs, three 300 ms warmup iterations and
five 300 ms measurement iterations, with the GC profiler and 256 MiB fork heap.
The result includes per-fork data, error estimates and normalized allocated bytes.
Results are written to `modules/benchmark/build/reports/jmh/results.json`.
Increase iteration duration/count for release-grade performance investigations;
short shared-runner measurements remain noisy and do not establish universal gains.

Cases cover codec-only round trips, YAML parsing/loading, reverse-order identity
move plus modification, and MINIMAL reduction, each with 16 and 256 elements and
Map entries. Fixtures are built in setup; save cases parse and load a NEW source
and session each invocation so they do not accidentally measure repeated no-op save.
Outputs are returned to JMH. Setup checks baseline and changed-value round trips.
The save cases include parse/load/formatting/in-memory transaction, but not disk I/O.

Ordinary build compiles the harness but does not execute timings. There is no
wall-clock threshold in correctness CI. The existing bounded replay tests and
profile diagnostics remain; JMH is not a correctness oracle.

Reference: https://github.com/openjdk/jmh
