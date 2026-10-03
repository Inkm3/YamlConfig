package com.github.inkm3.yamlconfig.save.internal.serialized.identity

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode
import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.IdentityHashMap

/** Original-list counts remain fixed even when a duplicate is later removed. */
internal class YamlSequenceIdentityMatcher private constructor(
    field: YamlIdentityField,
    baseline: List<YamlNode>,
    current: List<YamlNode>,
) {
    private val values = IdentityHashMap<YamlNode, YamlMapKey?>()
    private val oldCounts = counts(field, baseline)
    private val newCounts = counts(field, current)

    internal fun uniqueKey(node: YamlNode): YamlMapKey? {
        val id = values[node] ?: return null
        return id.takeIf { oldCounts[it] == 1 && newCounts[it] == 1 }
    }

    internal fun matches(before: YamlNode, after: YamlNode): Boolean {
        val id = uniqueKey(before) ?: return false
        return id == uniqueKey(after)
    }

    private fun counts(field: YamlIdentityField, nodes: List<YamlNode>): Map<YamlMapKey, Int> {
        val counts = HashMap<YamlMapKey, Int>()
        for (node in nodes) {
            val id = if (values.containsKey(node)) values[node] else field.value(node).also { values[node] = it }
            if (id != null) counts[id] = (counts[id] ?: 0) + 1
        }
        return counts
    }

    internal companion object {
        fun create(element: SerialDescriptor, baseline: List<YamlNode>, current: List<YamlNode>): YamlSequenceIdentityMatcher? =
            YamlIdentityField.resolve(element)?.let { YamlSequenceIdentityMatcher(it, baseline, current) }
    }
}
