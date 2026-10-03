package com.github.inkm3.yamlconfig.spi

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor

public interface YamlEditor {
    public val root: YamlNode?

    public fun set(path: YamlPath, node: YamlNode): Unit
    public fun remove(path: YamlPath): Unit
    public fun mapping(path: YamlPath): YamlMappingEditor
    public fun sequence(path: YamlPath): YamlSequenceEditor
    public fun fork(): YamlEditor
}