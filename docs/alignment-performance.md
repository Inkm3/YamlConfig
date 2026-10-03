# Plan-scoped source alignment reuse

A save plan can retry retained/ordinary/forced candidate construction against the
same original source. These attempts may repeat recursive collection alignment.
`YamlSourceAlignmentMemo` shares results across those attempts and nested plans.

The key is descriptor REFERENCE, baseline REFERENCE, source REFERENCE and omission
policy. The serial name and ordinary Node equality are not used as identity keys.
This prevents differently structured descriptors with the same name from sharing
an answer. Input nodes are immutable and descriptor/configuration behavior must
remain fixed/deterministic during a save, as already required by serialization.

At most 32 boolean results are kept, with oldest-entry eviction. Storage is lazy;
scalar and missing-value checks bypass it. This is an entry limit, not a byte
limit. It does not remove the full candidate or actual Editor decode/encode
validation, cache thrown failures, or retain anything across saves/sessions.
False alignment outcomes can be reused; unexpected exceptions propagate.

Tests cover repeated positive/negative outcomes, alias descriptors, omission
policy, new source references, bounded eviction and exception propagation.
Planner-level tests verify reuse across retries in one plan and independence
across subsequent plan invocations. The deterministic benefit is fewer repeated
alignment traversals, not a guaranteed elapsed-time improvement.

The combined index/memo JMH comparison in [sequence-performance.md](sequence-performance.md)
includes slower small-case measurements and extra allocation in the large case.
It does not isolate this memo's impact. One-shot saves may gain nothing and pay
additional bookkeeping cost. No standalone memo speedup percentage is claimed.
