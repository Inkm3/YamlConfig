package com.github.inkm3.yamlconfig.serialization.internal.decoder

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class YamlObjectNodeDecoder(
    format: YamlSerialization,
    mapping: YamlMappingNode,
    private val path: YamlPath,
    descriptor: SerialDescriptor,
) : YamlNodeDecoder(format) {
    private val iterator = mapping.entries.entries.iterator()
    private val seen = BooleanArray(descriptor.elementsCount)
    private var selectedNode: YamlNode = mapping
    private var selectedPath: YamlPath = path

    override fun currentNode(): YamlNode = selectedNode
    override fun currentPath(): YamlPath = selectedPath

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val childPath = path.child(entry.key)
            if (entry.key.kind != YamlScalarKind.STRING) {
                failYaml(childPath, "Object property names must be YAML strings")
            }
            val index = descriptor.getElementIndex(entry.key.value)
            if (index == CompositeDecoder.UNKNOWN_NAME) {
                if (format.ignoreUnknownKeys) continue
                failYaml(childPath, "Unknown property '${entry.key.value}' for ${descriptor.serialName}")
            }
            if (index !in seen.indices) failYaml(childPath, "Invalid descriptor element index: $index")
            if (seen[index]) failYaml(childPath, "Duplicate object property")
            seen[index] = true
            selectedNode = entry.value
            selectedPath = childPath
            return index
        }
        return CompositeDecoder.DECODE_DONE
    }
}
