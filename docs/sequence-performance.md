# Indexed Sequence matching

For lists with at least 128 elements on both sides, structural saving uses an
index of exact-value hash buckets and already validated unique identity keys.
Small lists retain the original allocation-light diff implementation.

Hash equality is never accepted as value equality. Every exact candidate is
compared, and the earliest current position wins, preserving the old exact-first
greedy ordering even with collisions. Identity extraction/counting still uses the
ORIGINAL baseline/current lists; deletion cannot make a duplicate unique later.
Index entries follow evolving positions and never contain raw unknown user data.

This reduces expensive value/identity searches for large unique lists. It does
NOT make every list operation O(n log n): ArrayList moves and position updates
remain quadratic in worst cases, and all-colliding buckets can still scan many
entries. There is additional O(n) indexing memory. No universal speed claim.

## Correctness

Tests compare the entire edit sequence, not only the final list, with the original
algorithm for 14,641 small nullable value-list pairs and 7,225 identity-list pairs,
including constant hashes. Large deterministic random mixed edits, original-list
immutability, duplicate-reference handling and comparison counts are checked too.
Real SnakeYAML tests cross the threshold and verify unknown-field ownership,
comments, lexemes, both save modes and no-op behavior. The candidate and actual
Editor still undergo whole-document validation. Public API signatures are unchanged.

## Reversed-order comparison on 2026-10-03

Baseline `5cba09ad` and current `08a572a1` were measured in GitLab pipeline #367,
job #975. The two pairs ran reference/current and then current/reference on the
same shared Runner, Java 17, JMH 1.37, 2 forks, 10 warmup and 10 measurement
iterations of 300 ms. The unchanged workload constructs a fresh in-memory YAML
source and session, loads, reverses the object List, updates the moved first
object, validates and saves to text. It includes parsing and emission, not disk I/O.
The current implementation combines the index and alignment memo; this experiment
does not isolate their individual effects.

| Pair | Elements | Reference us/op | Current us/op | Time change | Allocation change |
|---|---:|---:|---:|---:|---:|
| reference then current | 16 | 510.950 | 513.874 | +0.57% | +0.47% |
| reference then current | 256 | 8434.924 | 8276.067 | -1.88% | +1.26% |
| current then reference | 16 | 482.220 | 529.653 | +9.84% | -0.12% |
| current then reference | 256 | 7953.493 | 7530.263 | -5.32% | +1.20% |

Scores are JMH means, not medians. Negative time change means a smaller mean.
The first large-case uncertainty intervals overlap. These short shared-runner
experiments are observations, not a statistically established general speedup.
The 16-element case includes a measurable slower result; it must not be hidden.
Even though it bypasses the index, it still exercises the other save machinery.
The larger case trades about 1.2% more allocation for lower time point estimates.

The implementation is retained for its collision-safe reduction in repeated
searches, with the small-list fallback and documented memory/time tradeoffs. The
128-element threshold is a conservative policy, not a proven optimal crossover.
No correctness validation was removed to obtain these results. Pathological
hash collisions and ArrayList movement remain possible quadratic work.

[Machine-readable summary](measurements/2026-10-03-sequence-summary.json) records
exact scores, error fields, commits and job provenance. Raw per-fork samples,
manifest and environment are in that job's comparison artifact (subject to its
retention period). `ci/compare-sequence-jmh.sh` reproduces paired execution and
`ci/summarize-sequence-jmh.py` rejects incomparable configurations.

The earlier comparison pipeline #360 failed because the JDK container lacked
Git. Checkout was performed in a different Runner helper image. Commit c5472d04
prepared Git explicitly; #364 and #367 succeeded. This was not a library test
failure. See also [JMH usage](benchmarks.md) and [alignment reuse](alignment-performance.md).
