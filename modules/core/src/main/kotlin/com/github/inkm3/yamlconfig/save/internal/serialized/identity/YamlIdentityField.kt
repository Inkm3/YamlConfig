package com.github.inkm3.yamlconfig.save.internal.serialized.identity

import com.github.inkm3.yamlconfig.annotation.YamlIdentity
import com.github.inkm3.yamlconfig.annotation.YamlIdentityDisabled
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlScalarRepresentation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/** Descriptor convention, not reflection or a claim that required means stable. */
@OptIn(ExperimentalSerializationApi::class)
internal class YamlIdentityField private constructor(
    internal val key: YamlMapKey,
    private val descriptor: SerialDescriptor,
) {
    internal fun value(node: YamlNode): YamlMapKey? {
        val scalar = (node as? YamlMappingNode)?.get(key) as? YamlScalarNode ?: return null
        if (scalar.kind == YamlScalarKind.NULL) return null
        val normalized = YamlScalarRepresentation.normalize(descriptor, YamlMapKey(scalar.value, scalar.kind))
            ?: return null
        if (descriptor.kind == SerialKind.ENUM &&
            descriptor.getElementIndex(normalized.value) !in 0 until descriptor.elementsCount
        ) return null
        // Do not assign identity semantics to NaN or infinities.
        if (normalized.kind == YamlScalarKind.FLOAT &&
            normalized.value in setOf(".nan", ".inf", "-.inf")
        ) return null
        return normalized
    }

    internal companion object {
        fun resolve(element: SerialDescriptor): YamlIdentityField? {
            if (element.isInline ||
                (element.kind != StructureKind.CLASS && element.kind != StructureKind.OBJECT)
            ) return null
            val explicit = (0 until element.elementsCount).filter { index ->
                element.getElementAnnotations(index).any { it is YamlIdentity }
            }
            val disabled = element.annotations.any { it is YamlIdentityDisabled }
            if (explicit.size > 1 || (disabled && explicit.isNotEmpty())) throw SerializationException(
                "Conflicting YAML identity annotations on ${element.serialName}",
            )
            if (disabled) return null
            val index = explicit.singleOrNull() ?: (0 until element.elementsCount).firstOrNull {
                !element.isElementOptional(it) && supported(element.getElementDescriptor(it))
            } ?: return null
            val field = element.getElementDescriptor(index)
            if (!supported(field)) throw SerializationException(
                "YAML identity must be a non-inline scalar: ${element.serialName}.${element.getElementName(index)}",
            )
            return YamlIdentityField(YamlMapKey(element.getElementName(index)), field)
        }

        private fun supported(descriptor: SerialDescriptor): Boolean = !descriptor.isInline &&
            (descriptor.kind is PrimitiveKind || descriptor.kind == SerialKind.ENUM)
    }
}
