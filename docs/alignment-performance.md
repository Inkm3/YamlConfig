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
validation, cache failures, or retain anything across saves/sessions.

Tests cover repeated positive/negative outcomes, alias descriptors, omission
policy, new source references, bounded eviction and exception propagation.
The deterministic benefit is fewer repeated alignment traversals. Runtime and
allocation changes must be measured; one-shot saves may gain nothing.
