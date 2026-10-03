package com.github.inkm3.yamlconfig.snakeyaml.internal

import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal object SnakeYamlPresentation {

    internal fun copyMetadata(source: Node, target: Node): Unit {
        target.anchor = source.anchor
        target.blockComments = source.blockComments?.toList()
        target.inLineComments = source.inLineComments?.toList()
        target.endComments = source.endComments?.toList()
        target.isRecursive = source.isRecursive
    }

    internal fun preserveReplacement(source: Node, replacement: Node): Node {
        return when {
            source is ScalarNode &&
            replacement is ScalarNode &&
            source.tag == replacement.tag -> {
                ScalarNode(
                    replacement.tag,
                    replacement.value,
                    source.scalarStyle
                ).also { result ->
                    copyMetadata(source, result)
                }
            }

            source is SequenceNode &&
            replacement is SequenceNode -> {
                replacement.flowStyle = source.flowStyle
                copyMetadata(source, replacement)
                replacement
            }

            source is MappingNode &&
            replacement is MappingNode -> {
                replacement.flowStyle = source.flowStyle
                copyMetadata(source, replacement)
                replacement
            }

            else -> {
                copyMetadata(source, replacement)
                replacement
            }

        }
    }
}