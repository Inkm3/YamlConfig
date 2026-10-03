package com.github.inkm3.yamlconfig.serialization.internal.encoder

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.descriptors.SerialDescriptor

internal class YamlMapNodeEncoder(
    format: YamlSerialization,
    private val path: YamlPath,
    private val sink: (YamlNode) -> Unit,
) : YamlNodeEncoder(format) {
    private val entries = linkedMapOf<YamlMapKey, YamlNode>()
    private var nextIndex = 0
    private var pending = false
    private var key: YamlMapKey? = null
    private var closed = false

    override fun currentPath(): YamlPath = key?.let(path::child) ?: path.child(nextIndex / 2)

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (closed || pending) failYaml(currentPath(), "Unfinished or closed map serializer")
        if (index != nextIndex) failYaml(path, "Expected map index $nextIndex, got $index")
        pending = true
        return true
    }

    override fun emit(node: YamlNode) {
        if (!pending) failYaml(path, "Map value has no element index")
        if (nextIndex % 2 == 0) {
            val scalar = node as? YamlScalarNode
                ?: failYaml(currentPath(), "Only scalar YAML mapping keys are supported")
            val selected = YamlMapKey(scalar.value, scalar.kind)
            if (entries.containsKey(selected)) failYaml(path.child(selected), "Duplicate YAML mapping key")
            key = selected
        } else {
            val selected = key ?: failYaml(path, "Map value has no key")
            entries[selected] = node
            key = null
        }
        nextIndex++
        pending = false
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (closed || pending || key != null) failYaml(currentPath(), "Unfinished or closed map serializer")
        closed = true
        sink(YamlMappingNode(entries))
    }
}
