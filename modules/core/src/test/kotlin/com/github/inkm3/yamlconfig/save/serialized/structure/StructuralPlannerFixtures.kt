package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralValuePlanner
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.assertEquals

@Serializable
internal data class StructuralItem(val name: String, val port: Int = 25565)

internal object StructuralPlannerFixtures {
    inline fun <reified T> plan(before: T, after: T, user: YamlNode, force: Boolean = false): YamlStructuralEdit? {
        val format = YamlSerialization.Default
        return YamlStructuralValuePlanner.plan(serializer<T>().descriptor,
            format.encodeToNode(before), format.encodeToNode(after), user, force, root = true)
    }

    fun apply(user: YamlNode?, edit: YamlStructuralEdit?, expected: YamlNode?) {
        val initial = user.toString()
        val pure = if (edit == null) user else YamlStructuralNodeEditor.apply(user, edit)
        val editor = TestYamlEditor(user)
        if (edit != null) YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
        assertEquals(expected, pure)
        assertEquals(pure, editor.root)
        assertEquals(initial, user.toString())
    }
}
