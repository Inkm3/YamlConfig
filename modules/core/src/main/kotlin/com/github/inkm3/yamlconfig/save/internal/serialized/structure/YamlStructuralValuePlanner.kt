package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

@OptIn(ExperimentalSerializationApi::class)
internal object YamlStructuralValuePlanner {
    internal fun plan(
        descriptor: SerialDescriptor,
        baseline: YamlNode?,
        current: YamlNode?,
        user: YamlNode?,
        force: Boolean = false,
        root: Boolean = false,
        retainOmitted: Boolean = false,
        alignment: YamlSourceAlignmentMemo = YamlSourceAlignmentMemo(),
    ): YamlStructuralEdit? {
        if (!force && baseline == current) return null
        if (current == null) return if (user == null) null else YamlStructuralEdit.Set(null)
        if (isObject(descriptor) && current is YamlMappingNode &&
            (user is YamlMappingNode || (root && user == null))
        ) return objectEdit(descriptor, baseline as? YamlMappingNode, current, user as? YamlMappingNode,
            force, retainOmitted, alignment)
        if (!descriptor.isInline && descriptor.kind == StructureKind.LIST && baseline is YamlSequenceNode &&
            current is YamlSequenceNode && user is YamlSequenceNode &&
            alignment.matches(descriptor, baseline, user, retainOmitted)
        ) return YamlSequenceValuePlanner.plan(descriptor.getElementDescriptor(0), baseline, current, user,
            force, retainOmitted, alignment)
        if (!descriptor.isInline && descriptor.kind == StructureKind.MAP && baseline is YamlMappingNode &&
            current is YamlMappingNode && user is YamlMappingNode &&
            alignment.matches(descriptor, baseline, user, retainOmitted)
        ) return YamlMapValuePlanner.plan(descriptor, baseline, current, user, force, retainOmitted, alignment)
        if (user == current) return null
        if (current !is YamlMappingNode && current !is YamlSequenceNode &&
            alignment.matches(descriptor, current, user)
        ) return null
        return YamlStructuralEdit.Set(current)
    }

    private fun objectEdit(
        descriptor: SerialDescriptor, baseline: YamlMappingNode?, current: YamlMappingNode,
        user: YamlMappingNode?, force: Boolean, retainOmitted: Boolean, alignment: YamlSourceAlignmentMemo,
    ): YamlStructuralEdit? {
        val changes = LinkedHashMap<YamlMapKey, YamlStructuralEdit>()
        for (index in 0 until descriptor.elementsCount) {
            val key = YamlMapKey(descriptor.getElementName(index))
            if (retainOmitted && current[key] == null && descriptor.isElementOptional(index)) continue
            val edit = plan(descriptor.getElementDescriptor(index), baseline?.get(key), current[key],
                user?.get(key), force, retainOmitted = retainOmitted, alignment = alignment)
            if (edit != null) changes[key] = edit
        }
        return changes.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Mapping)
    }

    internal fun isObject(descriptor: SerialDescriptor): Boolean = !descriptor.isInline &&
        (descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT)
}
