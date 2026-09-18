package com.github.inkm3.yamlconfig.snakeyaml.internal

import org.snakeyaml.engine.v2.nodes.*
import java.util.*

internal object SnakeYamlNodeCopy {

    internal fun copy(node: Node): Node {
        return Copier().copy(node)
    }

    private class Copier {

        private val copies: IdentityHashMap<Node, Node> = IdentityHashMap()

        fun copy(source: Node): Node {
            copies[source]?.let { copied -> return copied }

            return when (source) {
                is ScalarNode -> copyScalar(source)
                is SequenceNode -> copySequence(source)
                is MappingNode -> copyMapping(source)

                else -> throw SnakeYamlDecodeException(
                    "Unsupported SnakeYAML node type: ${source::class.qualifiedName}"
                )
            }
        }

        private fun copyScalar(source: ScalarNode): ScalarNode {
            val target = ScalarNode(source.tag, source.value, source.scalarStyle)

            copies[source] = target
            SnakeYamlPresentation.copyMetadata(source, target)

            return target
        }

        private fun copySequence(source: SequenceNode): SequenceNode {
            val children = ArrayList<Node>(source.value.size)
            val target = SequenceNode(source.tag, children, source.flowStyle)

            copies[source] = target
            SnakeYamlPresentation.copyMetadata(source, target)

            for (child in source.value) {
                children += copy(child)
            }

            return target
        }

        private fun copyMapping(source: MappingNode): MappingNode {
            val tuples = ArrayList<NodeTuple>(source.value.size)
            val target = MappingNode(source.tag, tuples, source.flowStyle)

            copies[source] = target
            SnakeYamlPresentation.copyMetadata(source, target)

            for (tuple in source.value) {
                tuples += NodeTuple(copy(tuple.keyNode), copy(tuple.valueNode))
            }

            return target
        }
    }
}