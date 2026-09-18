package com.github.inkm3.yamlconfig.snakeyaml.document.editor

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.document.SnakeYamlEditor
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeEncoder
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlPresentation
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal class SnakeYamlSequenceEditor(
    private val editor: SnakeYamlEditor,
    private val path: YamlPath,
): YamlSequenceEditor {
    override val size: Int
        get() = sequence().value.size

    override fun replace(position: Int, node: YamlNode) {
        val sequence = sequence()
        requireExisting(sequence, position)

        val current = sequence.value[position]
        val replacement = SnakeYamlPresentation.preserveReplacement(
            current,
            SnakeYamlNodeEncoder.encode(node),
        )

        sequence.value[position] = replacement
    }

    override fun insert(position: Int, node: YamlNode) {
        val sequence = sequence()
        if (position < 0) {
            throw YamlEditException(
                path,
                "Sequence insertion index must not be negative: $position",
            )
        }

        if (position > sequence.value.size) {
            throw YamlEditException(
                path.child(position),
                buildString {
                    append("Sequence insertion index $position ")
                    append("is out of bounds (size: ${sequence.value.size})")
                }
            )
        }

        sequence.value.add(
            position,
            SnakeYamlNodeEncoder.encode(node),
        )
    }

    override fun remove(position: Int): Unit {
        val sequence = sequence()
        if (
            position < 0 ||
            position >= sequence.value.size
        ) {
            return
        }

        sequence.value.removeAt(position)
    }

    override fun move(fromIndex: Int, toIndex: Int) {
        val sequence = sequence()
        requireExisting(sequence, fromIndex)
        requireExisting(sequence, toIndex)

        if (fromIndex == toIndex) {
            return
        }


        val node = sequence.value.removeAt(fromIndex)
        sequence.value.add(toIndex, node)
    }

    private fun sequence(): SequenceNode {
        return editor.requireSequence(path)
    }

    private fun requireExisting(
        sequence: SequenceNode,
        index: Int,
    ) {
        if (index < 0) {
            throw YamlEditException(
                path,
                "Sequence index must not be negative: $index",
            )
        }

        if (index >= sequence.value.size) {
            throw YamlEditException(
                path.child(index),
                buildString {
                    append("Sequence index $index ")
                    append("is out of bounds (size: ${sequence.value.size})")
                }
            )
        }
    }
}