# Sequence element identity

For serializer-backed List/sequence saving, exact encoded-value matches are
tried first. A changed object can now be matched to an old object by a stable
scalar property and saved as Move followed by a recursive Update of the SAME
native node. Its unknown object entries and unchanged presentation travel with it.
This changes physical editing, not List loading or whole-override semantics.

## Implicit convention

```kotlin
@Serializable
data class Server(val name: String, val port: Int = 25565)
```

Here `name` is the first non-optional scalar in the element's SerialDescriptor.
The convention skips optional properties, complex types and inline value types,
then selects the first supported required property. Scalar means primitive or
enum, including a nullable scalar descriptor. Actual null, missing, malformed,
NaN and infinite identity values are not usable. A valid empty string is usable.
No second candidate is chosen when the selected field's value is missing/null.

Requiredness does NOT prove business uniqueness or stability. Applications must
choose an appropriate identity or disable this convention. Inserting/reordering
required scalar properties in a model can change the implicit selection.
`@Required` is considered according to descriptor optionality, not Kotlin source
syntax. Serialized names, including `@SerialName`, are used.

## Explicit choice and disabling

```kotlin
import com.github.inkm3.yamlconfig.annotation.YamlIdentity
import com.github.inkm3.yamlconfig.annotation.YamlIdentityDisabled

@Serializable
data class Entry(
    val displayName: String,
    @YamlIdentity @SerialName("entry-id") val id: Int = 0,
)

@Serializable
@YamlIdentityDisabled
data class PositionalEntry(val name: String, val value: Int)
```

An explicit field overrides the implicit convention and may have a constructor
default. Only one property can be annotated. Multiple identities, an unsupported
explicit type, or identity together with disabling cause SerializationException
when identity resolution is reached during saving. This is not eager schema
validation at load time; a no-op save may not invoke it.

Disabling applies to the annotated element class, not unrelated nested classes.
No candidate or disabling retains the old exact-value moves and positional
updates; it does NOT mean replace every element. Annotations use SerialInfo, so
custom serializers must expose the intended descriptor metadata themselves.
They do not add YAML keys or change the ordinary encode/decode representation.

## Matching and ambiguity

The identity must occur exactly once in BOTH original lists for a changed item
to match. Counts are not recomputed after deleting items. Duplicates of one id
do not disable matching of other unique ids. Repeated references still count as
multiple occurrences. Scalar spellings are normalized with the codec's rules.

With an active identity field:

- Exact normalized-node equality is still preferred. Equal duplicates use the
  existing deterministic value-based matching, not metadata as a hidden id.
- A unique unchanged identity permits Move + Update, or Update at the same index.
- An identity change is replacement, NOT rename. Null/missing/duplicate identity
  on changed values also disallows positional Update. Unmatched nodes are removed
  and inserted so old unknown fields/comments do not get attached to a new item.

Replacement/deletion intentionally loses the removed node's unknown data. This is
safer than claiming to preserve it on an unidentifiable/new item, but applications
that need that data should provide stable unique ids. Identity does not restore
information absent from the serialized baseline (for example an omitted field).

## Other boundaries

Source-to-baseline alignment remains required before structural edits. A missing
user collection, an incompatible source shape, or opaque/custom representation
can still use whole-value Set; presentation inside that replacement is not
promised. Identity is computed from encoded values and cannot certify arbitrary
transforming custom serializers or state that they never encode.

Same-defaults full-document reloading/encoding validation, MINIMAL_DIFFERENCE,
force repair for dependent defaults, forked editors and write-success-only state
updates remain in place. This work does not implement general omitted-default
overrides, thread safety, or concurrent external file reconciliation.

## Work and testing

A matcher belongs to one diff. Descriptor selection is done once per matcher;
identity extraction is cached by node reference and counts are indexed per list.
No global mutable descriptor/value cache is added. Sequence matching and ArrayList
moves can still be quadratic; no measured speedup is claimed.

Tests cover descriptor conventions/annotations, duplicates/null/invalid values,
identity diff replay, both structural appliers, SnakeYAML metadata ownership,
nested collections, both save modes, dependent defaults and failed-write retry.
One test exhaustively replays 7,225 pairs of small lists containing duplicate ids
and changed values. It is one JUnit method with bounded enumeration, not a proof
for all inputs or 7,225 separate JUnit methods.
