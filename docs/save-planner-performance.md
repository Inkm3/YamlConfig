# Save candidate profiling and bounded validation reuse

The test-only `ReferenceCandidatePlanner` freezes candidate selection/validation
at commit `bc407116`. It shares the unchanged structural patch builders, omission
pruner and minimal reducer with production. This comparison is specifically about
candidate validation; it is not an independent oracle for all saving behavior.

## Safety and lifetime

Production now constructs `YamlCandidateValidation` once per plan invocation.
It retains at most 16 input/result pairs, discarding the oldest evaluated entries
as capacity is reached. The first result needs no history list. This is an entry
count limit, NOT a fixed byte limit; large documents/results consume more memory.
All references become eligible for collection when planning completes/fails.
No memo is stored on a session, shared between calls, or reused for final Editor
validation. The actual forked Editor ALWAYS goes through a fresh decode/encode.

Only exact representations are equal cache inputs: scalar kind AND original
spelling, sequence order, and mapping entry order at every depth. The public Node
equality/hash contract is unchanged. A mapping's ordinary equals ignores order,
which is too weak for a custom deserializer that observes entry order. The memo
uses recursive comparison (with a reference fast path), not hash-only equality,
whole-document string allocation, or semantic normalization of its keys.

Successful results and SerializationException failures can be reused. Failures
retain their type and cause. Unexpected exceptions are neither swallowed nor
cached. Existing deterministic serializer/default requirements still apply;
applications must not depend on how many times decoding/constructors are called,
mutate serializer configuration during save, or rely on nondeterministic defaults.
No claim is made about transient/unencoded state or arbitrary Kotlin equals.

## Correctness tests

Tests compare successful candidate trees (including entry order), error types and
messages, reload counts, and replay through both the pure patch and mutating test
Editor. The frozen planner and production are also compared across 64 combinations
of dependent defaults, explicit/omitted fields and both save modes. This is bounded
coverage, not a proof for every custom serializer.

Unit tests cover null versus missing, scalar kind/lexeme, nested mapping order,
sequence order, hash collisions, successful/failed eviction, unexpected exceptions,
and empty memo per plan. Public API tests count both candidate and actual-Editor
validation, failed-write retry, session independence and no-op behavior. A real
custom serializer that reads the first mapping entry checks order-sensitive reuse.
Existing SnakeYAML presentation, default repair, identity and failure/retry tests
remain in the full build.

## Reproduce the optional timing diagnostic

```shell
./gradlew :core:test --tests '*SavePlannerProfileTest' -PprofileSavePlanner=true
```

The Gradle property is a declared test input. In this mode the core test task runs
without up-to-date or build-cache reuse, and standard output is logged. Normal
build/test retains caching and does not run timing loops. A CI run can provide
`ORG_GRADLE_PROJECT_profileSavePlanner=true` as a pipeline variable; the build
command and runtime dependencies are unchanged. The ref must be allowed by the
existing workflow rules (a branch without an open MR, or a manual web run).

Each case has 30 paired warmups, 7 samples of 20 operations per implementation,
and alternating control/current order. Output contains raw per-operation samples,
their median, JDK information, and full reload counts. Timing excludes fixture
construction and I/O, but includes candidate planning and decoding/encoding.
There are no wall-clock assertions. This is an in-process diagnostic, not JMH,
end-to-end latency prediction, allocation profiling or statistical proof. CI
noise, JIT and GC affect results. Reload-count assertions are deterministic.

## Measured first run (2026-10-02)

- Before optimization: commit 834db7bd, pipeline #295/job816. Production and control
  were the same algorithm. Timings nevertheless differed, demonstrating noise.
- After optimization: a26011df, pipeline #297/job818. Java 17.0.20.1+1, OpenJDK
  64-Bit Server VM, 4 reported processors. All test/compile tasks executed.
- The following values compare CONTROL and OPTIMIZED in that SAME optimized run,
  not unrelated absolute CI durations or different pipeline timings.

| Case | Full reloads before -> after | Control median ns/op | Optimized median ns/op |
|---|---:|---:|---:|
| scalar-preserve | 1 -> 1 | 6324 | 6629 |
| object-preserve | 1 -> 1 | 24463 | 23763 |
| dependent-repair | 2 -> 2 | 35340 | 35102 |
| omitted-retained | 1 -> 1 | 13191 | 12835 |
| omitted-unrepresentable | 6 -> 4 | 83550 | 69206 |
| large-unrepresentable | 6 -> 2 | 1246437 | 962502 |
| minimal-reduction | 13 -> 9 | 1202351 | 946452 |
| sequence-move-update | 1 -> 1 | 189166 | 186233 |

The large document contains 64 object elements and 64 Map entries. These are
planning-only measurements. In particular the unrepresentable cases still fail
before writing; a faster rejection is not a faster successful file save. The
scalar median was about 4.8% higher in this run; do not hide it or claim all cases
improved. Samples contain outliers. The deterministic result is fewer reloads
where candidate strategies revisit the same representations, not a universal
latency percentage. Repeated measurements and raw samples are retained in the
MR report; do not infer application speedup from pipeline duration.

## Remaining optimization targets

MINIMAL candidate generation/tree copying, descriptor/source alignment scans and
Sequence matching/ArrayList movement remain separate work. This change does not
optimize those algorithms or remove full-document correctness checks. A bounded
memo can miss old results after eviction and still incurs exact comparison cost.
