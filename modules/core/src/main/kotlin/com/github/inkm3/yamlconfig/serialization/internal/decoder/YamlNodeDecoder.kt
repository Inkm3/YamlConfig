package com.github.inkm3.yamlconfig.serialization.internal.decoder

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.atYamlPath
import com.github.inkm3.yamlconfig.serialization.internal.codec.YamlScalarLexicalCodec
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import com.github.inkm3.yamlconfig.serialization.internal.requireSupportedInline
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal abstract class YamlNodeDecoder(
    protected val format: YamlSerialization,
) : AbstractDecoder() {
    final override val serializersModule: SerializersModule
        get() = format.serializersModule

    protected abstract fun currentNode(): YamlNode
    protected abstract fun currentPath(): YamlPath

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE
    override fun decodeSequentially(): Boolean = false

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val node = currentNode()
        val path = currentPath()
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> YamlObjectNodeDecoder(
                format, node as? YamlMappingNode ?: failYaml(path, "Expected YAML mapping"), path, descriptor,
            )
            StructureKind.LIST -> YamlSequenceNodeDecoder(
                format, node as? YamlSequenceNode ?: failYaml(path, "Expected YAML sequence"), path,
            )
            StructureKind.MAP -> YamlMapNodeDecoder(
                format, node as? YamlMappingNode ?: failYaml(path, "Expected YAML mapping"), path,
            )
            else -> failYaml(path, "Unsupported structure kind: ${descriptor.kind}")
        }
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        atYamlPath(currentPath()) { deserializer.deserialize(this) }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        requireSupportedInline(descriptor, currentPath())
        return this
    }

    override fun decodeNotNullMark(): Boolean =
        (currentNode() as? YamlScalarNode)?.kind != YamlScalarKind.NULL

    override fun decodeNull(): Nothing? {
        val scalar = requireScalar(YamlScalarKind.NULL)
        if (!YamlScalarLexicalCodec.isNull(scalar.value)) failYaml(currentPath(), "Invalid null value")
        return null
    }

    override fun decodeBoolean(): Boolean =
        decode("Boolean", YamlScalarKind.BOOLEAN, YamlScalarLexicalCodec::decodeBoolean)
    override fun decodeByte(): Byte = decode("Byte", YamlScalarKind.INTEGER, YamlScalarLexicalCodec::decodeByte)
    override fun decodeShort(): Short = decode("Short", YamlScalarKind.INTEGER, YamlScalarLexicalCodec::decodeShort)
    override fun decodeInt(): Int = decode("Int", YamlScalarKind.INTEGER, YamlScalarLexicalCodec::decodeInt)
    override fun decodeLong(): Long = decode("Long", YamlScalarKind.INTEGER, YamlScalarLexicalCodec::decodeLong)
    override fun decodeString(): String = requireScalar(YamlScalarKind.STRING).value

    override fun decodeChar(): Char {
        val value = decodeString()
        if (value.length != 1) failYaml(currentPath(), "Expected exactly one UTF-16 character")
        return value[0]
    }

    override fun decodeFloat(): Float {
        val scalar = numericScalar()
        return YamlScalarLexicalCodec.decodeFloat(scalar.value, scalar.kind == YamlScalarKind.INTEGER)
            ?: failYaml(currentPath(), "Invalid or out-of-range Float: ${scalar.value}")
    }

    override fun decodeDouble(): Double {
        val scalar = numericScalar()
        return YamlScalarLexicalCodec.decodeDouble(scalar.value, scalar.kind == YamlScalarKind.INTEGER)
            ?: failYaml(currentPath(), "Invalid or out-of-range Double: ${scalar.value}")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val value = decodeString()
        val index = enumDescriptor.getElementIndex(value)
        if (index !in 0 until enumDescriptor.elementsCount) {
            failYaml(currentPath(), "Unknown enum value '$value' for ${enumDescriptor.serialName}")
        }
        return index
    }

    private fun numericScalar(): YamlScalarNode {
        val node = currentNode() as? YamlScalarNode ?: failYaml(currentPath(), "Expected numeric YAML scalar")
        if (node.kind != YamlScalarKind.FLOAT && node.kind != YamlScalarKind.INTEGER) {
            failYaml(currentPath(), "Expected FLOAT or INTEGER, got ${node.kind}")
        }
        return node
    }

    private fun requireScalar(kind: YamlScalarKind): YamlScalarNode {
        val node = currentNode() as? YamlScalarNode ?: failYaml(currentPath(), "Expected YAML scalar")
        if (node.kind != kind) failYaml(currentPath(), "Expected $kind, got ${node.kind}")
        return node
    }

    private inline fun <T> decode(type: String, kind: YamlScalarKind, parse: (String) -> T?): T {
        val scalar = requireScalar(kind)
        return parse(scalar.value) ?: failYaml(currentPath(), "Invalid or out-of-range $type: ${scalar.value}")
    }
}
