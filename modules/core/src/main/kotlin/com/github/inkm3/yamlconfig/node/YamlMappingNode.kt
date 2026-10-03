package com.github.inkm3.yamlconfig.node

import java.util.*

public class YamlMappingNode(
    entries: Map<YamlMapKey, YamlNode>,
): YamlNode {

    public val entries: Map<YamlMapKey, YamlNode> = Collections.unmodifiableMap(LinkedHashMap(entries))

    public val size: Int get() = entries.size

    public operator fun get(key: YamlMapKey): YamlNode? {
        return entries[key]
    }

    public operator fun get(key: String): YamlNode? {
        return entries[YamlMapKey(key, YamlScalarKind.STRING)]
    }

    public fun containsKey(key: YamlMapKey): Boolean {
        return key in entries
    }

    public fun containsKey(key: String): Boolean {
        return YamlMapKey(key, YamlScalarKind.STRING) in entries
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is YamlMappingNode && entries == other.entries
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }

    override fun toString(): String {
        return "YamlMappingNode(entries=$entries)"
    }
}