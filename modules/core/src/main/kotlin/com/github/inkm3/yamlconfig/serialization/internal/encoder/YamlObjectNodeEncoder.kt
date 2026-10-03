package com.github.inkm3.yamlconfig.serialization.internal.encoder

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.descriptors.SerialDescriptor

internal class YamlObjectNodeEncoder(
    format: YamlSerialization,
    private val path: YamlPath,
    private val sink: (YamlNode) -> Unit,
) : YamlNodeEncoder(format) {
    private val entries = linkedMapOf<YamlMapKey, YamlNode>()
    private var key: YamlMapKey? = null
    private var closed = false

    override fun currentPath(): YamlPath = key?.let(path::child) ?: path

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (closed || key != null) failYaml(currentPath(), "Unfinished or closed object serializer")
        if (index !in 0 until descriptor.elementsCount) failYaml(path, "Invalid object index: $index")
        val selected = YamlMapKey(descriptor.getElementName(index))
        if (entries.containsKey(selected)) failYaml(path.child(selected), "Duplicate object property")
        key = selected
        return true
    }

    override fun emit(node: YamlNode) {
        val selected = key ?: failYaml(path, "Object value has no property name")
        entries[selected] = node
        key = null
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (closed || key != null) failYaml(currentPath(), "Unfinished or closed object serializer")
        closed = true
        sink(YamlMappingNode(entries))
    }
}
