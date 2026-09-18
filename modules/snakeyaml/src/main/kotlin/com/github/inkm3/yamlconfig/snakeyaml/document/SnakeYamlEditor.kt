package com.github.inkm3.yamlconfig.snakeyaml.document

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.document.editor.SnakeYamlMappingEditor
import com.github.inkm3.yamlconfig.snakeyaml.document.editor.SnakeYamlSequenceEditor
import com.github.inkm3.yamlconfig.snakeyaml.internal.*
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal class SnakeYamlEditor(
    nativeRoot: Node?,
): YamlEditor {

    private var rootNode: Node? = nativeRoot

    override val root: YamlNode? get() = rootNode?.let(SnakeYamlNodeDecoder::decode)

    internal val nativeRoot: Node? get() = rootNode

    override fun set(path: YamlPath, node: YamlNode): Unit {
        val replacement = SnakeYamlNodeEncoder.encode(node)
        if (path.isRoot) {
            rootNode = rootNode?.let { current ->
                SnakeYamlPresentation.preserveReplacement(
                    current,
                    replacement
                )
            } ?: replacement

            return
        }

        ensureRootForSet(path)

        val target = SnakeYamlPathNavigator.forSet(
            requireNotNull(rootNode),
            path,
        )

        when (val segment = target.segment) {
            is YamlPath.Segment.Key -> setMappingValue(
                target.parent,
                target.parentPath,
                segment,
                replacement,
            )

            is YamlPath.Segment.Index -> setSequenceValue(
                target.parent,
                target.parentPath,
                segment,
                replacement,
            )
        }
    }

    override fun remove(path: YamlPath): Unit {
        if (path.isRoot) {
            rootNode = null
            return
        }

        val root = rootNode ?: return
        val target = SnakeYamlPathNavigator.findTarget(root, path)
            ?: return

        when (val segment = target.segment) {
            is YamlPath.Segment.Key -> {
                val mapping = target.parent as? MappingNode
                    ?: throw SnakeYamlNodes.expectedMapping(
                        target.parentPath,
                        target.parent,
                    )

                val index = SnakeYamlNodes.findKeyIndex(mapping, segment.key)
                if (index >= 0) {
                    mapping.value.removeAt(index)
                }
            }

            is YamlPath.Segment.Index -> {
                val sequence = target.parent as? SequenceNode
                    ?: throw SnakeYamlNodes.expectedSequence(
                        target.parentPath,
                        target.parent,
                    )

                if (segment.index < sequence.value.size) {
                    sequence.value.removeAt(segment.index)
                }
            }
        }
    }

    override fun mapping(path: YamlPath): YamlMappingEditor {
        requireMapping(path)

        return SnakeYamlMappingEditor(this, path)
    }

    override fun sequence(path: YamlPath): YamlSequenceEditor {
        requireSequence(path)

        return SnakeYamlSequenceEditor(this, path)
    }

    override fun fork(): YamlEditor {
        return SnakeYamlEditor(rootNode?.let(SnakeYamlNodeCopy::copy))
    }

    internal fun requireMapping(path: YamlPath): MappingNode {
        val node = SnakeYamlPathNavigator.requireNode(rootNode, path)

        return node as? MappingNode
            ?: throw SnakeYamlNodes.expectedMapping(path, node)
    }

    internal fun requireSequence(path: YamlPath): SequenceNode {
        val node = SnakeYamlPathNavigator.requireNode(rootNode, path)

        return node as? SequenceNode
            ?: throw SnakeYamlNodes.expectedSequence(path, node)
    }

    private fun ensureRootForSet(path: YamlPath): Unit {
        if (rootNode != null) {
            return
        }

        if (path.segments.first() !is YamlPath.Segment.Key) {
            throw YamlEditException(
                YamlPath.root(),
                "Cannot create a missing root sequence while setting $path",
            )
        }

        rootNode = SnakeYamlNodes.newMapping()
    }

    private fun setMappingValue(
        parent: Node,
        parentPath: YamlPath,
        segment: YamlPath.Segment.Key,
        replacement: Node,
    ): Unit {
        val mapping = parent as? MappingNode
            ?: throw SnakeYamlNodes.expectedMapping(
                parentPath,
                parent,
            )

        val index = SnakeYamlNodes.findKeyIndex(mapping, segment.key)
        if (index < 0) {
            mapping.value.add(
                NodeTuple(
                    SnakeYamlNodeEncoder.encodeKey(segment.key),
                    replacement,
                )
            )

            return
        }

        val existing = mapping.value[index]

        mapping.value[index] = NodeTuple(
            existing.keyNode,
            SnakeYamlPresentation.preserveReplacement(
                existing.valueNode,
                replacement,
            )
        )
    }

    private fun setSequenceValue(
        parent: Node,
        parentPath: YamlPath,
        segment: YamlPath.Segment.Index,
        replacement: Node,
    ): Unit {
        val sequence = parent as? SequenceNode
            ?: throw SnakeYamlNodes.expectedSequence(parentPath, parent)

        if (segment.index >= sequence.value.size) {
            throw YamlEditException(
                parentPath.child(segment.index),
                buildString {
                    append("Sequence index ${segment.index} ")
                    append("is out of bounds at ${parentPath.child(segment.index)} ")
                    append("(size: ${sequence.value.size})")
                },
            )
        }

        val existing = sequence.value[segment.index]
        sequence.value[segment.index] = SnakeYamlPresentation.preserveReplacement(
            existing,
            replacement,
        )
    }
}