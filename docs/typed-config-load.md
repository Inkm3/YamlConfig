# Typed Config loading

Typed loading is implemented by `YamlNodeOverlay` and `YamlConfigValueLoader` and
is used by the public serializer-backed `yamlConfig<T>(...)` API.

```text
defaultsRoot + userRoot + descriptor
              |
        descriptor-aware overlay
              |
       one serializer decode
              |
              T
```

## Selection rules

- Kotlin null in the internal overlay means a missing node.
- YAML null is an explicit `YamlScalarNode(NULL)`.
- Missing user values inherit YAML defaults.
- Non-inline CLASS / OBJECT values merge known serialized properties recursively.
- List / Map / inline / opaque contextual values are whole-value overrides.
- `@SerialName` names are used.
- Invalid user values are not repaired from defaults; decoding reports them.
- Missing nested fields stay missing so the serializer decides constructor defaults and required fields.

An entirely missing non-nullable non-inline object root may be bootstrapped as `{}`
so constructor defaults can run. Missing scalar/collection/nullable/inline roots are
not silently invented.

Unknown string properties are ignored for typed Config decoding but remain in the
original document/editor and are preserved by later saves where alignment permits.
