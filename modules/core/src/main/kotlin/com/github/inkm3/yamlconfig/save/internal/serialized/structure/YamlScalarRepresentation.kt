package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.serialization.internal.codec.YamlScalarLexicalCodec
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind

/** Codec-compatible spellings, not application identity or custom key inference. */
@OptIn(ExperimentalSerializationApi::class)
internal object YamlScalarRepresentation {
    internal fun normalize(descriptor: SerialDescriptor, key: YamlMapKey): YamlMapKey? {
        if (descriptor.isInline) return key
        if (key.kind == YamlScalarKind.NULL) return if (descriptor.isNullable &&
            YamlScalarLexicalCodec.isNull(key.value)
        ) YamlMapKey("null", YamlScalarKind.NULL) else null
        val text = key.value
        val number = when (descriptor.kind) {
            PrimitiveKind.BYTE -> YamlScalarLexicalCodec.decodeByte(text)?.toString()
            PrimitiveKind.SHORT -> YamlScalarLexicalCodec.decodeShort(text)?.toString()
            PrimitiveKind.INT -> YamlScalarLexicalCodec.decodeInt(text)?.toString()
            PrimitiveKind.LONG -> YamlScalarLexicalCodec.decodeLong(text)?.toString()
            else -> null
        }
        return when (descriptor.kind) {
            PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG ->
                number?.takeIf { key.kind == YamlScalarKind.INTEGER }?.let { YamlMapKey(it, YamlScalarKind.INTEGER) }
            PrimitiveKind.BOOLEAN -> if (key.kind == YamlScalarKind.BOOLEAN) {
                YamlScalarLexicalCodec.decodeBoolean(text)?.let { YamlMapKey(it.toString(), YamlScalarKind.BOOLEAN) }
            } else null
            PrimitiveKind.FLOAT -> if (key.kind == YamlScalarKind.INTEGER || key.kind == YamlScalarKind.FLOAT) {
                YamlScalarLexicalCodec.decodeFloat(text, key.kind == YamlScalarKind.INTEGER)?.let {
                    YamlMapKey(YamlScalarLexicalCodec.encodeFloat(it), YamlScalarKind.FLOAT)
                }
            } else null
            PrimitiveKind.DOUBLE -> if (key.kind == YamlScalarKind.INTEGER || key.kind == YamlScalarKind.FLOAT) {
                YamlScalarLexicalCodec.decodeDouble(text, key.kind == YamlScalarKind.INTEGER)?.let {
                    YamlMapKey(YamlScalarLexicalCodec.encodeDouble(it), YamlScalarKind.FLOAT)
                }
            } else null
            PrimitiveKind.STRING, SerialKind.ENUM -> key.takeIf { it.kind == YamlScalarKind.STRING }
            PrimitiveKind.CHAR -> key.takeIf { it.kind == YamlScalarKind.STRING && it.value.length == 1 }
            // Opaque/custom representations only get exact-key matching.
            else -> key
        }
    }

    /** A collision is ambiguity, never last-writer-wins. */
    internal fun index(descriptor: SerialDescriptor, keys: Set<YamlMapKey>): Map<YamlMapKey, YamlMapKey>? {
        val result = LinkedHashMap<YamlMapKey, YamlMapKey>()
        for (key in keys) {
            val canonical = normalize(descriptor, key) ?: return null
            if (result.put(canonical, key) != null) return null
        }
        return result
    }
}
