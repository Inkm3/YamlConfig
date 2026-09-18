package com.github.inkm3.yamlconfig.schema.collection

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlKeySchema
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.schema.internal.YamlSchemaSupport

internal class YamlMapSchema<K, V>(
    internal val keySchema: YamlKeySchema<K>,
    internal val valueSchema: YamlSchema<V>
): YamlSchema<Map<K, V>>() {

    override fun decode(node: YamlNode, path: YamlPath): Map<K, V> {
        if (node !is YamlMappingNode) {
            throw YamlSchemaException(path, "Expected mapping, but found ${YamlSchemaSupport.nodeType(node)}")
        }

        val result = linkedMapOf<K, V>()
        for ((yamlKey, yamlValue) in node.entries) {
            val entryPath = path.child(yamlKey)
            val key = keySchema.decodeKey(yamlKey, entryPath)
            if (result.containsKey(key)) {
                throw YamlSchemaException(entryPath, "Duplicate mapping key after decoding")
            }

            val value = valueSchema.decode(yamlValue, entryPath)
            result[key] = value
        }

        return result
    }

    override fun encode(value: Map<K, V>): YamlNode {
        val entries = linkedMapOf<YamlMapKey, YamlNode>()
        for ((key, mapValue) in value) {
            val yamlKey = keySchema.encodeKey(key)

            require(yamlKey !in entries) {
                "Duplicate mapping key after encoding: $yamlKey"
            }

            entries[yamlKey] = valueSchema.encode(mapValue)
        }

        return YamlMappingNode(entries)
    }

    override fun equivalent(first: Map<K, V>, second: Map<K, V>): Boolean {
        if (first.size != second.size) {
            return false
        }

        for ((key, firstValue) in first) {
            if (!second.containsKey(key)) {
                return false
            }

            val secondValue = second.getValue(key)
            if (!valueSchema.equivalent(firstValue, secondValue)) {
                return false
            }
        }

        return true
    }
}