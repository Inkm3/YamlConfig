package com.github.inkm3.yamlconfig.serialization.internal.encoder

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.descriptors.SerialDescriptor

internal class YamlSequenceNodeEncoder(
    format: YamlSerialization,
    private val path: YamlPath,
    private val sink: (YamlNode) -> Unit,
) : YamlNodeEncoder(format) {
    private val elements = arrayListOf<YamlNode>()
    private var pending = false
    private var closed = false

    override fun currentPath(): YamlPath = if (pending) path.child(elements.size) else path

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (closed || pending) failYaml(currentPath(), "Unfinished or closed sequence serializer")
        if (index != elements.size) failYaml(path, "Expected sequence index ${elements.size}, got $index")
        pending = true
        return true
    }

    override fun emit(node: YamlNode) {
        if (!pending) failYaml(path, "Sequence value has no element index")
        elements += node
        pending = false
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (closed || pending) failYaml(currentPath(), "Unfinished or closed sequence serializer")
        closed = true
        sink(YamlSequenceNode(elements))
    }
}
