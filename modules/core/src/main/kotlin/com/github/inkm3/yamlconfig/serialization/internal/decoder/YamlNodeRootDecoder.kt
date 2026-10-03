package com.github.inkm3.yamlconfig.serialization.internal.decoder

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization

internal class YamlNodeRootDecoder(
    format: YamlSerialization,
    private val node: YamlNode,
) : YamlNodeDecoder(format) {
    override fun currentNode(): YamlNode = node
    override fun currentPath(): YamlPath = YamlPath.root()
}
