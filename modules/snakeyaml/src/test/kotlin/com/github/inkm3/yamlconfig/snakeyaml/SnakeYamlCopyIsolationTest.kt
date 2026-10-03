package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnakeYamlCopyIsolationTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun documentEditorsAreIndependent() {
        val document = engine.parse(StringYamlInput("value: original"))
        val first = document.editor()
        val second = document.editor()

        first.set(
            YamlPath.root().child("value"),
            YamlScalarNode("changed", YamlScalarKind.STRING),
        )

        val firstRoot = assertIs<YamlMappingNode>(first.root)
        val secondRoot = assertIs<YamlMappingNode>(second.root)
        assertEquals(YamlScalarNode("changed", YamlScalarKind.STRING), firstRoot["value"])
        assertEquals(YamlScalarNode("original", YamlScalarKind.STRING), secondRoot["value"])
    }

    @Test
    fun forkIsIndependentFromOriginalEditor() {
        val editor = engine.parse(StringYamlInput("nested: {value: original}")).editor()
        val fork = editor.fork()

        fork.set(
            YamlPath.root().child("nested").child("value"),
            YamlScalarNode("changed", YamlScalarKind.STRING),
        )

        val originalRoot = assertIs<YamlMappingNode>(editor.root)
        val forkRoot = assertIs<YamlMappingNode>(fork.root)
        val originalNested = assertIs<YamlMappingNode>(originalRoot["nested"])
        val forkNested = assertIs<YamlMappingNode>(forkRoot["nested"])
        assertEquals(YamlScalarNode("original", YamlScalarKind.STRING), originalNested["value"])
        assertEquals(YamlScalarNode("changed", YamlScalarKind.STRING), forkNested["value"])
    }

    @Test
    fun editingDocumentEditorDoesNotMutateDocumentLogicalRoot() {
        val document = engine.parse(StringYamlInput("value: original"))
        val rootBefore = document.root
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("value"),
            YamlScalarNode("changed", YamlScalarKind.STRING),
        )

        assertEquals(rootBefore, document.root)
    }
}
