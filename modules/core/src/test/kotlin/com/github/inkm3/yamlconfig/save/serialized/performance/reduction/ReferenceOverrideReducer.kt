package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlKnownValuePlanner
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlValuePatch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/** Frozen reducer at d92aa225. Keep traversal/replay independent of its replacement.
 * Uses the existing general-purpose patch applier, which this optimization does not change.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object ReferenceOverrideReducer {
    fun reduce(descriptor: SerialDescriptor, initial: YamlNode?, matches: (YamlNode?) -> Boolean): List<YamlValuePatch> {
        var root = initial
        val removals = mutableListOf<YamlValuePatch>()
        do {
            val paths = mutableListOf<List<YamlMapKey>>()
            candidates(descriptor, root, emptyList(), paths)
            var changed = false
            for (keys in paths) {
                val patch = YamlValuePatch(keys, null)
                val candidate = patch.applyTo(root)
                if (candidate != root && matches(candidate)) {
                    root = candidate
                    removals += patch
                    changed = true
                }
            }
        } while (changed)
        return removals
    }

    private fun candidates(descriptor: SerialDescriptor, node: YamlNode?, keys: List<YamlMapKey>,
        paths: MutableList<List<YamlMapKey>>) {
        if (node == null) return
        if (!hasUnknown(descriptor, node)) paths += keys
        if (!YamlKnownValuePlanner.isObject(descriptor) || node !is YamlMappingNode) return
        for (index in 0 until descriptor.elementsCount) {
            val key = YamlMapKey(descriptor.getElementName(index))
            candidates(descriptor.getElementDescriptor(index), node[key], keys + key, paths)
        }
    }

    private fun hasUnknown(descriptor: SerialDescriptor, node: YamlNode): Boolean {
        if (YamlKnownValuePlanner.isObject(descriptor) && node is YamlMappingNode) {
            for ((key, value) in node.entries) {
                val index = descriptor.getElementIndex(key.value)
                if (key.kind != YamlScalarKind.STRING || index !in 0 until descriptor.elementsCount) return true
                if (hasUnknown(descriptor.getElementDescriptor(index), value)) return true
            }
        } else if (descriptor.kind == StructureKind.LIST && node is YamlSequenceNode) {
            return node.elements.any { hasUnknown(descriptor.getElementDescriptor(0), it) }
        } else if (descriptor.kind == StructureKind.MAP && node is YamlMappingNode) {
            return node.entries.values.any { hasUnknown(descriptor.getElementDescriptor(1), it) }
        }
        return false
    }
}
