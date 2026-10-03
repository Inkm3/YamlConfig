# Omitted properties: preserve candidates, do not invent values

This capability adds no YamlConfig annotation or public format setting. Ordinary
`@Serializable` properties need no extra annotation. The save path still requests
`encodeDefaults=true` internally and respects standard serializer behavior.

## What changed

An optional object property absent from the current encoding is no longer
immediately treated as a deletion in the preferred save candidate. Its original
user value can remain in the document. The whole candidate is then loaded with
the same YAML defaults and encoded again. It is accepted only when that result
equals the encoded current value, exactly as before.

For example, a user's explicit `port: 25565` can be essential against YAML defaults
`port: 30000`, even if `@EncodeDefault(NEVER)` omits the property at the Kotlin
default 25565. A force pass used to repair another dependent property must not
unconditionally delete this valid port override.

A retained value is not blindly trusted. If it reappears in the reloaded encoding
but is absent from the expected encoding, the planner can remove that optional
property and validate again. With dependent defaults, removing one property may
expose another mismatch; each pass strictly removes an existing property, so the
process terminates on finite documents. Unrelated retained omissions stay intact.

The ordinary and force-retention attempts each start from the original user tree.
If neither succeeds, the previous ordinary/force deletion strategies are tried.
No candidate is published without full validation. MINIMAL_DIFFERENCE still runs
its validated reduction only after a valid candidate has been found.

## Collections, identity and presentation

Retention flows into existing aligned List/Map values. Source alignment may ignore
an optional property missing from encoded baseline, but does not relax list
length, known encoded values, identity rules, non-string object keys or map-key
correspondence. Opaque and inline representations remain atomic.

Pruning inside Lists uses the already-updated candidate indices and does not
rematch or remove elements. Map pruning uses normalized correspondence and the
original raw key for editing. Entries removed from a Map or List are still removed
as data, not preserved as optional object properties. Identity replacement never
copies an unrelated old element's retained fields into a new element.

Retained values preserve their native nodes, including lexical spelling, comments
and unknown children. A property that actually must be deleted cannot retain its
subtree's comments/unknown data. Full-value fallback for unalignable/custom
representations keeps its existing presentation limitations.

## What this does NOT recover

A serializer may not provide the desired value at all. Suppose Kotlin's port
default is 25565, YAML defaults supply 30000, user YAML contains 40000, and the caller
changes the value to 25565 while using `@EncodeDefault(NEVER)`. The encoded current
object does not contain 25565. Keeping 40000 is wrong, and deleting it restores
30000. This implementation still rejects that save before writing. It does not
search arbitrary numbers, inspect private fields, add a compiler plugin, or use
an independent annotation to duplicate the model's default value.

Prefer removing unnecessary `@EncodeDefault(NEVER)` from configuration models.
When a model is shared with another format and needs an explicit choice, standard
`kotlinx.serialization.EncodeDefault(ALWAYS)` makes that value available:

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Config(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val port: Int = 25565,
)
```

The format's public encoding contract is unchanged. ALWAYS/NEVER are serializer
choices, not overridden by YamlConfig. Custom serializers must expose the state
they intend to persist. Equality remains equality of the serializer's normalized
representation, not proof about unencoded/transient state or arbitrary equals.
`isElementOptional` provides optionality, NOT the property's default value.

## Safety and cost

All new operations occur in pure candidate planning. The same patch trees are
applied to the forked Editor, followed by the existing full-document validation
before write. Failures do not advance the session baseline. No cross-save cache
of omitted values is introduced, and unexpected validator exceptions are not
swallowed. There is no reflective field access or new library dependency.

The added strategies can cost extra full decodes on a rejected candidate. This is
bounded candidate validation, not exhaustive search for every representable YAML.
No claim of global minimality or measured speed improvement is made. Profiling
and safe removal of repeated work remain follow-up optimization tasks.

## Primary references

- https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/-mode/-n-e-v-e-r/
- https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/-mode/-a-l-w-a-y-s/
- https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/is-element-optional.html

These references define standard annotation/descriptor behavior. The preservation
and pruning strategies above are YamlConfig's implementation, tested separately.
