package com.github.inkm3.yamlconfig.save.internal.serialized

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.spi.YamlEditor

/** Shared patch representation, including the reducer's property removals. */
internal class YamlValuePatch private constructor(private val edit: YamlStructuralEdit) {
    internal constructor(keys: List<YamlMapKey>, value: YamlNode?) : this(
        keys.asReversed().fold<YamlMapKey, YamlStructuralEdit>(YamlStructuralEdit.Set(value)) { child, key ->
            YamlStructuralEdit.Mapping(mapOf(key to child))
        },
    )

    internal fun apply(editor: YamlEditor) = YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
    internal fun applyTo(root: YamlNode?): YamlNode? = YamlStructuralNodeEditor.apply(root, edit)

    internal companion object {
        internal fun structural(edit: YamlStructuralEdit): YamlValuePatch = YamlValuePatch(edit)
    }
}
