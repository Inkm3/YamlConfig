package com.github.inkm3.yamlconfig.serialization.internal.encoder

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.atYamlPath
import com.github.inkm3.yamlconfig.serialization.internal.codec.YamlScalarLexicalCodec
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import com.github.inkm3.yamlconfig.serialization.internal.requireSupportedInline
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal abstract class YamlNodeEncoder(
    protected val format: YamlSerialization,
) : AbstractEncoder() {
    final override val serializersModule: SerializersModule
        get() = format.serializersModule

    protected abstract fun emit(node: YamlNode)
    protected abstract fun currentPath(): YamlPath

    private fun scalar(value: String, kind: YamlScalarKind) {
        emit(YamlScalarNode(value, kind))
    }

    override fun encodeBoolean(value: Boolean) = scalar(value.toString(), YamlScalarKind.BOOLEAN)
    override fun encodeByte(value: Byte) = scalar(value.toString(), YamlScalarKind.INTEGER)
    override fun encodeShort(value: Short) = scalar(value.toString(), YamlScalarKind.INTEGER)
    override fun encodeInt(value: Int) = scalar(value.toString(), YamlScalarKind.INTEGER)
    override fun encodeLong(value: Long) = scalar(value.toString(), YamlScalarKind.INTEGER)
    override fun encodeChar(value: Char) = scalar(value.toString(), YamlScalarKind.STRING)
    override fun encodeString(value: String) = scalar(value, YamlScalarKind.STRING)
    override fun encodeFloat(value: Float) =
        scalar(YamlScalarLexicalCodec.encodeFloat(value), YamlScalarKind.FLOAT)
    override fun encodeDouble(value: Double) =
        scalar(YamlScalarLexicalCodec.encodeDouble(value), YamlScalarKind.FLOAT)
    override fun encodeNull() = scalar("null", YamlScalarKind.NULL)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        if (index !in 0 until enumDescriptor.elementsCount) {
            failYaml(currentPath(), "Invalid enum index: $index")
        }
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        requireSupportedInline(descriptor, currentPath())
        return this
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        format.encodeDefaults

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        atYamlPath(currentPath()) { serializer.serialize(this, value) }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val path = currentPath()
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> YamlObjectNodeEncoder(format, path, ::emit)
            StructureKind.LIST -> YamlSequenceNodeEncoder(format, path, ::emit)
            StructureKind.MAP -> YamlMapNodeEncoder(format, path, ::emit)
            else -> failYaml(path, "Unsupported structure kind: ${descriptor.kind}")
        }
    }
}
