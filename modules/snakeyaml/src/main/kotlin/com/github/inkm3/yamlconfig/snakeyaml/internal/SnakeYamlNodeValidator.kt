package com.github.inkm3.yamlconfig.snakeyaml.internal

import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.*
import java.util.*

internal object SnakeYamlNodeValidator {

    internal fun validate(root: Node): Unit {
        Validator().validate(root)
    }

    private class Validator {

        private val visited: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        private val active: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())

        fun validate(node: Node) {
            visit(node)
        }

        private fun visit(node: Node) {
            rejectUnsupportedNode(node)

            when {
                node.isRecursive -> throw SnakeYamlDecodeException(
                    "Recursive YAML aliases are not supported",
                )

                node.anchor.isPresent -> throw SnakeYamlDecodeException(
                    "YAML anchors and aliases are not supported",
                )

                !active.add(node) -> throw SnakeYamlDecodeException(
                    "Recursive YAML aliases are not supported",
                )

                !visited.add(node) -> throw SnakeYamlDecodeException(
                    "YAML aliases are not supported",
                )

                else -> try {
                    visitChildren(node)
                } finally {
                    active.remove(node)
                }
            }
        }

        private fun visitChildren(node: Node): Unit {
            when (node) {
                is ScalarNode -> Unit
                is SequenceNode -> node.value.forEach(::visit)
                is MappingNode -> node.value.forEach { tuple ->
                    rejectMergeKey(tuple.keyNode)

                    visit(tuple.keyNode)
                    visit(tuple.valueNode)
                }


                else -> throw SnakeYamlDecodeException(
                    "Unsupported YAML node type: ${node::class.java.name}",
                )
            }
        }

        private fun rejectMergeKey(keyNode: Node) {
            val scalar = keyNode as? ScalarNode
                ?: return

            if (isMergeKey(scalar)) {
                throw SnakeYamlDecodeException(
                    "YAML merge keys are not supported",
                )
            }
        }

        private fun isMergeKey(node: ScalarNode): Boolean {
            if (node.tag == Tag.MERGE) {
                return true
            }

            return (
                node.value == "<<" &&
                node.scalarStyle == ScalarStyle.PLAIN
            )
        }

        private fun rejectUnsupportedNode(node: Node) {
            if (node is AnchorNode) {
                throw SnakeYamlDecodeException(
                    "YAML aliases are not supported",
                )
            }
        }
    }
}