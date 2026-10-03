package com.github.inkm3.yamlconfig.save.internal.serialized.reduction

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode

/**
 * Pure removal of one root/object path, with no intermediate edit tree.
 * Missing paths return the SAME node. Only ancestors of a present deletion are
 * copied; empty parents are retained. Existing Node defensive copies remain.
 */
internal object YamlRemovalCandidate {
    internal fun remove(root: YamlNode?, keys: List<YamlMapKey>): YamlNode? = remove(root, keys, 0)

    private fun remove(root: YamlNode?, keys: List<YamlMapKey>, index: Int): YamlNode? {
        if (index == keys.size) return null
        if (root == null) return null
        require(root is YamlMappingNode) { "Expected mapping for structural edit" }
        val key = keys[index]
        val previous = root[key] ?: return root
        val current = remove(previous, keys, index + 1)
        if (current === previous) return root
        val entries = LinkedHashMap(root.entries)
        if (current == null) entries.remove(key) else entries[key] = current
        return YamlMappingNode(entries)
    }
}
