package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.node.*
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal object SnakeYamlNodeDecoder {

    internal fun decode(node: Node): YamlNode {
        return when (node) {
            is ScalarNode -> decodeScalar(node)
            is SequenceNode -> decodeSequence(node)
            is MappingNode -> decodeMapping(node)

            else -> throw SnakeYamlDecodeException("Unsupported SnakeYAML node type: ${node::class.java.name}")
        }
    }

    private fun decodeScalar(node: ScalarNode): YamlScalarNode {
        return YamlScalarNode(
            node.value,
            SnakeYamlScalarCodec.decodeKind(node.tag),
        )
    }

    private fun decodeSequence(node: SequenceNode): YamlSequenceNode {
        return YamlSequenceNode(node.value.map(::decode))
    }

    private fun decodeMapping(node: MappingNode): YamlMappingNode {
        val entries = LinkedHashMap<YamlMapKey, YamlNode>(node.value.size)
        for (tuple in node.value) {
            val key = decodeKey(tuple.keyNode)
            val value = decode(tuple.valueNode)

            if (entries.containsKey(key)) {
                throw SnakeYamlDecodeException("Duplicate YAML mapping key: $key")
            }

            entries[key] = value
        }

        return YamlMappingNode(entries)
    }

    internal fun decodeKey(node: Node): YamlMapKey {
        if (node !is ScalarNode) {
            throw SnakeYamlDecodeException("Complex YAML mapping keys are not supported")
        }

        return YamlMapKey(
            node.value,
            SnakeYamlScalarCodec.decodeKind(node.tag),
        )
    }
}