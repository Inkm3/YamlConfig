package com.github.inkm3.yamlconfig.context

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.path.YamlPathResolver
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor

internal class YamlWriteContext(
    private val editor: YamlEditor,
) {

    internal fun lookup(path: YamlPath): YamlNodeLookupResult {
        return YamlPathResolver.lookup(editor.root, path)
    }

    internal fun set(path: YamlPath, node: YamlNode): Unit {
        editor.set(path, node)
    }

    internal fun remove(path: YamlPath): Unit {
        editor.remove(path)
    }

    internal fun mapping(path: YamlPath): YamlMappingEditor {
        return editor.mapping(path)
    }

    internal fun sequence(path: YamlPath): YamlSequenceEditor {
        return editor.sequence(path)
    }
}