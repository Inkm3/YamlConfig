package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import kotlinx.serialization.descriptors.SerialDescriptor

/** One plan only. Descriptor AND node references are keys; serialName is not unique. */
internal class YamlSourceAlignmentMemo(
    private val capacity: Int = 32,
    private val evaluate: (SerialDescriptor, YamlNode?, YamlNode?, Boolean) -> Boolean = YamlSourceAlignment::matches,
) {
    private class Key(val descriptor: SerialDescriptor, val baseline: YamlNode, val user: YamlNode, val omitted: Boolean) {
        override fun equals(other: Any?): Boolean = other is Key && descriptor === other.descriptor &&
            baseline === other.baseline && user === other.user && omitted == other.omitted
        override fun hashCode(): Int = 31 * (31 * (31 * System.identityHashCode(descriptor) +
            System.identityHashCode(baseline)) + System.identityHashCode(user)) + omitted.hashCode()
    }
    private var entries: LinkedHashMap<Key, Boolean>? = null
    init { require(capacity > 0) }

    internal fun matches(descriptor: SerialDescriptor, baseline: YamlNode?, user: YamlNode?,
        allowOmitted: Boolean = false): Boolean {
        if (baseline === user) return true
        // Do not allocate a map or key for scalar-only or missing-value checks.
        if (baseline == null || user == null ||
            (baseline !is YamlMappingNode && baseline !is YamlSequenceNode)
        ) return evaluate(descriptor, baseline, user, allowOmitted)
        val key = Key(descriptor, baseline, user, allowOmitted)
        entries?.get(key)?.let { return it }
        val result = evaluate(descriptor, baseline, user, allowOmitted)
        val map = entries ?: LinkedHashMap<Key, Boolean>().also { entries = it }
        if (map.size == capacity) map.entries.iterator().let { it.next(); it.remove() }
        map[key] = result
        return result
    }
}
