# Structural collection saving

Load semantics remain whole-value override for List and Map. Save semantics can be
more surgical when the encoded baseline and source representation can be aligned.

## List

The save planner can emit Insert / Remove / Move / Update operations. Updates recurse
into known object fields so unknown fields and presentation on the same existing
element can survive. Sequence identity can extend matching to moved-and-modified
objects; see [sequence identity](sequence-identity.md).

## Map

Scalar keys are normalized for matching while the surviving source entry keeps its
raw key representation. Updates recurse into values. A key rename is remove + insert
and does not transfer unknown data from the removed entry.

## Fallback

If source alignment cannot be established, or a collection is inherited only from
defaults and does not exist in user YAML, saving falls back to whole-value Set.

Candidate construction and native Editor application use the same structural edit
model, followed by whole-document validation. SnakeYAML tests cover values, comments,
unknown-key ownership, raw key/scalar spellings, both save modes and retry behavior.
