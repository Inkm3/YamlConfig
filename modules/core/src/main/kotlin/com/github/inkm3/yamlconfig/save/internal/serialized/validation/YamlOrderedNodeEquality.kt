package com.github.inkm3.yamlconfig.save.internal.serialized.validation

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode

/**
 * Exact representation equality, including mapping iteration order at ALL depths.
 * YamlMappingNode.equals intentionally ignores order, but custom deserializers
 * can observe it. Never reuse validation on that weaker equality or hash alone.
 */
internal object YamlOrderedNodeEquality {
    internal fun same(left: YamlNode?, right: YamlNode?): Boolean {
        if (left === right) return true
        return when (left) {
            null -> false
            is YamlScalarNode -> left == right
            is YamlSequenceNode -> right is YamlSequenceNode && left.size == right.size &&
                left.elements.indices.all { same(left[it], right[it]) }
            is YamlMappingNode -> {
                if (right !is YamlMappingNode || left.size != right.size) return false
                val other = right.entries.entries.iterator()
                left.entries.all { (key, value) ->
                    val entry = other.next()
                    key == entry.key && same(value, entry.value)
                }
            }
        }
    }
}
