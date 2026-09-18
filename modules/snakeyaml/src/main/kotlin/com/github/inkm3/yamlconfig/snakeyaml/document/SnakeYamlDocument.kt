package com.github.inkm3.yamlconfig.snakeyaml.document

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeCopy
import com.github.inkm3.yamlconfig.spi.YamlDocument
import com.github.inkm3.yamlconfig.spi.YamlEditor
import org.snakeyaml.engine.v2.nodes.Node

internal class SnakeYamlDocument(
    private val nativeRoot: Node?,
    override val root: YamlNode?,
): YamlDocument {

    override fun editor(): YamlEditor {
        return SnakeYamlEditor(nativeRoot?.let(SnakeYamlNodeCopy::copy))
    }
}