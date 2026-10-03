package com.github.inkm3.yamlconfig.save.internal.serialized.omission

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlScalarRepresentation
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSequenceStep
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignment
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralValuePlanner
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Proposes deleting ONLY optional properties absent from expected encoding but
 * present after reloading a retained candidate. No value is inferred. The caller
 * must validate the entire document again. Collection entries are never deleted
 * as omissions; their contents may contain omitted object properties.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object YamlOmittedPropertyPruner {
    internal fun plan(
        descriptor: SerialDescriptor, expected: YamlNode?, actual: YamlNode?, source: YamlNode?,
    ): YamlStructuralEdit? {
        if (expected == null || actual == null || source == null || expected == actual || descriptor.isInline) return null
        if (YamlStructuralValuePlanner.isObject(descriptor) && expected is YamlMappingNode &&
            actual is YamlMappingNode && source is YamlMappingNode
        ) return objectEdit(descriptor, expected, actual, source)
        if (!YamlSourceAlignment.matches(descriptor, actual, source, allowOmitted = true)) return null
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                if (expected !is YamlSequenceNode || actual !is YamlSequenceNode ||
                    source !is YamlSequenceNode || expected.size != actual.size || actual.size != source.size
                ) return null
                val steps = mutableListOf<YamlSequenceStep>()
                for (index in expected.elements.indices) {
                    val edit = plan(descriptor.getElementDescriptor(0), expected[index], actual[index], source[index])
                    if (edit != null) steps += YamlSequenceStep.Update(index, edit)
                }
                steps.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Sequence)
            }
            StructureKind.MAP -> {
                if (expected !is YamlMappingNode || actual !is YamlMappingNode || source !is YamlMappingNode) return null
                mapEdit(descriptor, expected, actual, source)
            }
            else -> null
        }
    }

    private fun objectEdit(
        descriptor: SerialDescriptor, expected: YamlMappingNode, actual: YamlMappingNode, source: YamlMappingNode,
    ): YamlStructuralEdit? {
        val edits = LinkedHashMap<YamlMapKey, YamlStructuralEdit>()
        for (index in 0 until descriptor.elementsCount) {
            val key = YamlMapKey(descriptor.getElementName(index))
            val raw = source[key] ?: continue
            val wanted = expected[key]
            val observed = actual[key]
            val edit = if (wanted == null && observed != null && descriptor.isElementOptional(index)) {
                YamlStructuralEdit.Set(null)
            } else {
                plan(descriptor.getElementDescriptor(index), wanted, observed, raw)
            }
            if (edit != null) edits[key] = edit
        }
        return edits.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Mapping)
    }

    private fun mapEdit(
        descriptor: SerialDescriptor, expected: YamlMappingNode, actual: YamlMappingNode, source: YamlMappingNode,
    ): YamlStructuralEdit? {
        val keyDescriptor = descriptor.getElementDescriptor(0)
        val wanted = YamlScalarRepresentation.index(keyDescriptor, expected.entries.keys) ?: return null
        val observed = YamlScalarRepresentation.index(keyDescriptor, actual.entries.keys) ?: return null
        val raw = YamlScalarRepresentation.index(keyDescriptor, source.entries.keys) ?: return null
        if (wanted.keys != observed.keys || observed.keys != raw.keys) return null
        val edits = LinkedHashMap<YamlMapKey, YamlStructuralEdit>()
        for ((key, rawKey) in raw) {
            val edit = plan(descriptor.getElementDescriptor(1), expected[wanted.getValue(key)],
                actual[observed.getValue(key)], source[rawKey])
            if (edit != null) edits[rawKey] = edit
        }
        return edits.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Mapping)
    }
}
