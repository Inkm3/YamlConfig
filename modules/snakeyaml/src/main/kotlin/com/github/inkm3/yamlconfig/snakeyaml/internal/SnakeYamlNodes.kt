package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.path.YamlPath
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.nodes.*

internal object SnakeYamlNodes {

    internal fun newMapping(): MappingNode {
        return MappingNode(Tag.MAP, mutableListOf(), FlowStyle.BLOCK)
    }

    internal fun findKeyIndex(mapping: MappingNode, key: YamlMapKey): Int {
        return mapping.value.indexOfFirst { tuple ->
            SnakeYamlNodeDecoder.decodeKey(tuple.keyNode) == key
        }
    }

    internal fun expectedMapping(path: YamlPath, actual: Node): YamlEditException {
        return YamlEditException(
            path,
            "Expected mapping at $path, but found ${typeName(actual)}",
        )
    }

    internal fun expectedSequence(path: YamlPath, actual: Node): YamlEditException {
        return YamlEditException(
            path,
            "Expected sequence at $path, but found ${typeName(actual)}",
        )
    }

    private fun typeName(node: Node): String {
        return when (node) {
            is ScalarNode -> "scalar"
            is SequenceNode -> "sequence"
            is MappingNode -> "mapping"

            else -> node::class.simpleName ?: "unknown"
        }
    }
}