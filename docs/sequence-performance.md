# Indexed Sequence matching

For lists with at least 128 elements on both sides, structural saving uses an
index of exact-value hash buckets and already validated unique identity keys.
Small lists retain the original allocation-light implementation.

Hash equality is never accepted as value equality. Every exact candidate is
compared, and the earliest current position wins, preserving the old exact-first
greedy ordering even with collisions. Identity extraction/counting still uses the
ORIGINAL baseline/current lists; deletion cannot make a duplicate unique later.
Index entries follow evolving positions and never contain raw unknown user data.

This reduces expensive value/identity searches for large unique lists. It does
NOT make every list operation O(n log n): ArrayList moves and position updates
remain quadratic in worst cases, and all-colliding buckets can still scan many
entries. There is additional O(n) indexing memory. No universal speed claim.

Tests compare the entire edit sequence, not only the final list, with the original
algorithm for 14,641 small nullable value-list pairs and 7,225 identity-list pairs,
including constant hashes. Large deterministic random mixed edits, original-list
immutability, duplicate-reference handling and comparison counts are checked too.
Real SnakeYAML tests cross the threshold and verify unknown-field ownership,
comments, lexemes, both save modes and no-op behavior.

JMH comparison must use the same workload, JVM flags and fixture sizes before and
after connection. CI duration itself is not a benchmark. See benchmarks.md.
