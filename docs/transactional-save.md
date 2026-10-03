# Serializer-backed Config and transactional saving

Public Config loading/saving is serializer-backed.

```kotlin
@Serializable
data class Config(val port: Int = 25565)

val session = yamlConfig<Config>(engine, source).load()
session.value = session.value.copy(port = 30000)
session.save()
```

An explicit `KSerializer<T>` is accepted as the third argument. Reified lookup uses
the supplied `YamlSerialization.serializersModule`, including contextual registration.

## Transaction boundary

A save uses a detached encoded baseline and advances session state only after write succeeds:

```text
encode current
  -> plan candidate edits
  -> reload/encode whole candidate against defaults
  -> fork Editor
  -> apply edits
  -> reload/encode actual Editor root
  -> write
  -> publish Editor + baseline together
```

No-op `PRESERVE_OVERRIDES` saves do not fork or write.

Preparation, editing, validation or write failure keeps the previous successful Editor
and baseline, so the same session can be retried. Reentrant save is rejected.
Sessions require external synchronization.

The library-level transaction protects in-memory session state. Rollback of bytes
already handed to an output is the responsibility of the `YamlEngine` / `YamlOutput`
implementation.

## Equality contract

Save equality is equality of the serializer's normalized `YamlNode` representation,
not arbitrary Kotlin `equals`. Transient or unencoded state is outside this contract.
Serializer/default behavior is expected to be deterministic.

See [usage](usage.md) for save modes and [omitted properties](omitted-properties.md)
for serializer omissions.
