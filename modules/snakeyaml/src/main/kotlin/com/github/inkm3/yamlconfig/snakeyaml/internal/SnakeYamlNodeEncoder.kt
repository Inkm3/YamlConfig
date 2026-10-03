package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.node.*
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.nodes.*

internal object SnakeYamlNodeEncoder {

    internal fun encode(node: YamlNode): Node {
        return when (node) {
            is YamlScalarNode -> encodeScalar(node)
            is YamlSequenceNode -> encodeSequence(node)
            is YamlMappingNode -> encodeMapping(node)
        }
    }

    private fun encodeScalar(node: YamlScalarNode): ScalarNode {
        return ScalarNode(
            SnakeYamlScalarCodec.encodeTag(node.kind),
            node.value,
            SnakeYamlScalarStyle.valueStyle(node.kind),
        )
    }

    private fun encodeSequence(node: YamlSequenceNode): SequenceNode {
        return SequenceNode(Tag.SEQ, node.elements.map(::encode), FlowStyle.BLOCK)
    }

    private fun encodeMapping(node: YamlMappingNode): MappingNode {
        return MappingNode(
            Tag.MAP,
            node.entries.map { (key, value) ->
                NodeTuple(encodeKey(key), encode(value))
            },
            FlowStyle.BLOCK
        )
    }

    internal fun encodeKey(key: YamlMapKey): ScalarNode {
        return ScalarNode(
            SnakeYamlScalarCodec.encodeTag(key.kind),
            key.value,
            SnakeYamlScalarStyle.keyStyle(key),
        )
    }
}