package com.github.inkm3.yamlconfig.serialization.internal.decoder

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class YamlMapNodeDecoder(
    format: YamlSerialization,
    private val mapping: YamlMappingNode,
    private val path: YamlPath,
) : YamlNodeDecoder(format) {
    // Keep the entry iterator rather than allocating a second list of all entries.
    private val iterator = mapping.entries.entries.iterator()
    private var entry: Map.Entry<YamlMapKey, YamlNode>? = null
    private var nextIndex = 0
    private var selectedNode: YamlNode = mapping
    private var selectedPath: YamlPath = path
    private val decodedKeys = HashSet<Any?>()

    override fun currentNode(): YamlNode = selectedNode
    override fun currentPath(): YamlPath = selectedPath

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextIndex % 2 == 0) {
            if (!iterator.hasNext()) return CompositeDecoder.DECODE_DONE
            entry = iterator.next()
            val key = entry!!.key
            selectedNode = YamlScalarNode(key.value, key.kind)
            selectedPath = path.child(key)
        } else {
            selectedNode = entry!!.value
        }
        return nextIndex++
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = mapping.size

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        val value = super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
        // For example, raw INTEGER keys "1" and "+1" must not overwrite each other
        // after both have been decoded to the same Kotlin Int key.
        if (index % 2 == 0 && !decodedKeys.add(value)) {
            failYaml(selectedPath, "Duplicate decoded map key")
        }
        return value
    }
}
