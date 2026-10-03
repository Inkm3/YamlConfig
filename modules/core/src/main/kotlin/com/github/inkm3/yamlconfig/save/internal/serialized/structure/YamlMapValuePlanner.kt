package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import kotlinx.serialization.descriptors.SerialDescriptor

/** Preserve the original raw key and its native key node for surviving entries. */
internal object YamlMapValuePlanner {
    internal fun plan(
        descriptor: SerialDescriptor,
        baseline: YamlMappingNode,
        current: YamlMappingNode,
        user: YamlMappingNode,
        force: Boolean,
        retainOmitted: Boolean = false,
        alignment: YamlSourceAlignmentMemo = YamlSourceAlignmentMemo(),
    ): YamlStructuralEdit? {
        val keyDescriptor = descriptor.getElementDescriptor(0)
        val before = YamlScalarRepresentation.index(keyDescriptor, baseline.entries.keys)
        val after = YamlScalarRepresentation.index(keyDescriptor, current.entries.keys)
        val source = YamlScalarRepresentation.index(keyDescriptor, user.entries.keys)
        if (before == null || after == null || source == null || before.keys != source.keys) {
            return YamlStructuralEdit.Set(current)
        }
        val valueDescriptor = descriptor.getElementDescriptor(1)
        val changes = LinkedHashMap<YamlMapKey, YamlStructuralEdit>()
        for ((key, raw) in source) {
            if (key !in after) changes[raw] = YamlStructuralEdit.Set(null)
        }
        for ((key, newRaw) in after) {
            val oldRaw = source[key]
            if (oldRaw == null) {
                changes[newRaw] = YamlStructuralEdit.Set(current.entries.getValue(newRaw))
            } else {
                val edit = YamlStructuralValuePlanner.plan(valueDescriptor,
                    baseline[before.getValue(key)], current[newRaw], user[oldRaw], force,
                    retainOmitted = retainOmitted, alignment = alignment)
                if (edit != null) changes[oldRaw] = edit
            }
        }
        return changes.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Mapping)
    }
}
