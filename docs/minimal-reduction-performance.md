# MINIMAL candidate construction performance

`MINIMAL_DIFFERENCE` uses greedy, whole-document-validated override removal.
The reducer collects safe removal candidates once, protects subtrees containing
unknown information, and applies accepted removals with deletion-specific ancestor copies.

Scalar/missing roots use a fast path and do not build a candidate list.

The optimization preserves candidate validation order and returns the already
validated final root so the caller does not replay all accepted patches merely to
reconstruct the same candidate. Patches are still retained for application to the
real Editor, and the actual Editor root is revalidated before write.

Profiling is opt-in:

```shell
./gradlew :core:test --tests '*ProfileTest' -PprofileSavePlanner=true
```

Timing diagnostics have no CI pass/fail threshold. Thread allocation counters, when
available, are current-thread allocated bytes rather than retained heap. Measurements
do not include file parsing or output I/O and are not a JMH/statistical guarantee.

This work does not change serializer omission semantics, identity semantics,
source-alignment fallback, or the transactional safety boundary.
