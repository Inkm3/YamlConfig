package com.github.inkm3.yamlconfig.save.internal.serialized.reduction

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlKnownValuePlanner
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Discover removal paths once, in descriptor preorder. Only known properties
 * without unknown descendants may disappear; this eligibility cannot change
 * while applying ONLY these deletions. Collections remain whole-value candidates.
 * Descriptors must be stable during a reduction. No cache outlives this traversal.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object YamlRemovalCandidates {
    internal fun collect(descriptor: SerialDescriptor, root: YamlNode?): List<List<YamlMapKey>> {
        if (root == null) return emptyList()
        val paths = ArrayList<List<YamlMapKey>?>()
        visit(descriptor, root, emptyList(), paths)
        return paths.filterNotNull()
    }

    private fun visit(descriptor: SerialDescriptor, node: YamlNode, keys: List<YamlMapKey>,
        paths: MutableList<List<YamlMapKey>?>): Boolean {
        // Reserve preorder position, decide parent eligibility after its children.
        // A nullable slot avoids shifting all descendants when inserting a parent.
        val slot = paths.size
        paths.add(null)
        val unknown = if (YamlKnownValuePlanner.isObject(descriptor) && node is YamlMappingNode) {
            var found = false
            for ((key, value) in node.entries) {
                val index = descriptor.getElementIndex(key.value)
                if (key.kind != YamlScalarKind.STRING || index !in 0 until descriptor.elementsCount ||
                    (descriptor.getElementName(index) != key.value &&
                        hasUnknown(descriptor.getElementDescriptor(index), value))
                ) {
                    found = true
                    break
                }
                // Canonical child names are inspected by visit below. A custom
                // descriptor alias has no canonical removal path, but its unknown
                // descendants must still protect this parent, as in the old scan.
            }
            for (index in 0 until descriptor.elementsCount) {
                val key = YamlMapKey(descriptor.getElementName(index))
                val child = node[key] ?: continue
                if (visit(descriptor.getElementDescriptor(index), child, keys + key, paths)) found = true
            }
            found
        } else hasUnknown(descriptor, node)
        if (!unknown) paths[slot] = keys
        return unknown
    }

    // Collection contents affect protection but are not individual removal paths.
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
