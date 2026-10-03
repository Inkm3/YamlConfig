package com.github.inkm3.yamlconfig.serialization.internal.decoder

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class YamlSequenceNodeDecoder(
    format: YamlSerialization,
    private val sequence: YamlSequenceNode,
    private val path: YamlPath,
) : YamlNodeDecoder(format) {
    private var nextIndex = 0
    private var selectedIndex = -1

    override fun currentNode(): YamlNode =
        if (selectedIndex < 0) sequence else sequence.elements[selectedIndex]
    override fun currentPath(): YamlPath =
        if (selectedIndex < 0) path else path.child(selectedIndex)

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextIndex >= sequence.size) return CompositeDecoder.DECODE_DONE
        selectedIndex = nextIndex++
        return selectedIndex
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = sequence.size
}
