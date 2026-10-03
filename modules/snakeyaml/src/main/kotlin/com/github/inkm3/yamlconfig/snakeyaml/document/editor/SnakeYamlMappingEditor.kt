package com.github.inkm3.yamlconfig.snakeyaml.document.editor

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.document.SnakeYamlEditor
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeEncoder
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodes
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlPresentation
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.NodeTuple

internal class SnakeYamlMappingEditor(
    private val editor: SnakeYamlEditor,
    private val path: YamlPath,
    ): YamlMappingEditor {

    override fun replace(
        position: YamlMapKey,
        node: YamlNode
    ): Unit {
        val mapping = mapping()
        val index = SnakeYamlNodes.findKeyIndex(
            mapping,
            position,
        )
        if (index < 0) {
            throw YamlEditException(
                path.child(position),
                "Mapping key does not exist",
            )
        }

        val tuple = mapping.value[index]
        val replacement = SnakeYamlPresentation.preserveReplacement(
            tuple.valueNode,
            SnakeYamlNodeEncoder.encode(node),
        )

        mapping.value[index] = NodeTuple(tuple.keyNode, replacement)
    }

    override fun insert(
        position: YamlMapKey,
        node: YamlNode
    ): Unit {
        val mapping = mapping()

        if (
            SnakeYamlNodes.findKeyIndex(
                mapping,
                position,
            ) >= 0
        ) {
            throw YamlEditException(
                path.child(position),
                "Mapping key already exists",
            )
        }

        mapping.value.add(NodeTuple(
            SnakeYamlNodeEncoder.encodeKey(position),
            SnakeYamlNodeEncoder.encode(node),
        ))
    }

    override fun remove(position: YamlMapKey): Unit {
        val mapping = mapping()
        val index = SnakeYamlNodes.findKeyIndex(
            mapping,
            position,
        )
        if (index >= 0) {
            mapping.value.removeAt(index)
        }
    }

    private fun mapping(): MappingNode {
        return editor.requireMapping(path)
    }
}