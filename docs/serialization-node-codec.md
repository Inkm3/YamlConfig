# Serialization / YamlNode codec

`YamlSerialization` is the semantic codec used by the public Config API.

```text
@Serializable T
   <-> KSerializer
   <-> YamlSerialization
   <-> semantic YamlNode
```

It supports strict scalar kinds, objects, lists, scalar-key maps, nullable values,
enums, `@SerialName`, constructor defaults, contextual/custom serializers and
path-aware errors.

String-to-number coercion is not performed. Complex map keys, unsupported
polymorphic structures and unsupported inline representations fail explicitly.

The codec is semantic; SnakeYAML presentation preservation is handled by the
Editor layer during saving.
