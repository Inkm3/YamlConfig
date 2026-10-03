package com.github.inkm3.yamlconfig.serialization.internal.encoder

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml

internal class YamlNodeRootEncoder(format: YamlSerialization) : YamlNodeEncoder(format) {
    private var node: YamlNode? = null

    override fun currentPath(): YamlPath = YamlPath.root()

    override fun emit(node: YamlNode) {
        if (this.node != null) failYaml(currentPath(), "Serializer emitted multiple root values")
        this.node = node
    }

    fun result(): YamlNode = node ?: failYaml(currentPath(), "Serializer did not emit a YAML value")
}
