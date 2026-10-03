package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import kotlin.test.*

class SnakeYamlEditorTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun setAtRootReplacesRoot() {
        val editor = engine.parse(StringYamlInput("value: 1")).editor()
        val replacement = YamlScalarNode("hello", YamlScalarKind.STRING)

        editor.set(YamlPath.root(), replacement)

        assertEquals(replacement, editor.root)
    }

    @Test
    fun setOnEmptyDocumentCreatesMissingMappingParents() {
        val editor = engine.parse(StringYamlInput("")).editor()
        editor.set(
            YamlPath.root().child("server").child("database").child("host"),
            YamlScalarNode("localhost", YamlScalarKind.STRING),
        )

        val root = assertIs<YamlMappingNode>(editor.root)
        val server = assertIs<YamlMappingNode>(root["server"])
        val database = assertIs<YamlMappingNode>(server["database"])
        assertEquals(YamlScalarNode("localhost", YamlScalarKind.STRING), database["host"])
    }

    @Test
    fun setDoesNotAutomaticallyCreateMissingSequence() {
        val editor = engine.parse(StringYamlInput("root: {}" )).editor()

        assertFailsWith<YamlEditException> {
            editor.set(
                YamlPath.root().child("root").child("items").child(0),
                YamlScalarNode("value", YamlScalarKind.STRING),
            )
        }
    }

    @Test
    fun setReplacesExistingSequenceElement() {
        val editor = engine.parse(StringYamlInput("items: [a, b, c]")).editor()
        editor.set(
            YamlPath.root().child("items").child(1),
            YamlScalarNode("x", YamlScalarKind.STRING),
        )

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(YamlScalarNode("x", YamlScalarKind.STRING), items[1])
    }

    @Test
    fun setRejectsOutOfBoundsSequenceIndex() {
        val editor = engine.parse(StringYamlInput("items: [a]")).editor()
        val exception = assertFailsWith<YamlEditException> {
            editor.set(
                YamlPath.root().child("items").child(1),
                YamlScalarNode("x", YamlScalarKind.STRING),
            )
        }

        assertEquals(YamlPath.root().child("items").child(1), exception.path)
    }

    @Test
    fun removeRootClearsDocument() {
        val editor = engine.parse(StringYamlInput("value: 1")).editor()
        editor.remove(YamlPath.root())
        assertNull(editor.root)
    }

    @Test
    fun removeMissingMappingPathIsNoOp() {
        val editor = engine.parse(StringYamlInput("value: 1")).editor()
        val before = editor.root
        editor.remove(YamlPath.root().child("missing"))
        assertEquals(before, editor.root)
    }

    @Test
    fun removeSequenceElementShiftsRemainingElements() {
        val editor = engine.parse(StringYamlInput("items: [a, b, c]")).editor()
        editor.remove(YamlPath.root().child("items").child(1))

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(
            listOf(
                YamlScalarNode("a", YamlScalarKind.STRING),
                YamlScalarNode("c", YamlScalarKind.STRING),
            ),
            items.elements,
        )
    }

    @Test
    fun mappingRequiresMappingNode() {
        val editor = engine.parse(StringYamlInput("value: scalar")).editor()
        val exception = assertFailsWith<YamlEditException> {
            editor.mapping(YamlPath.root().child("value"))
        }
        assertEquals(YamlPath.root().child("value"), exception.path)
    }

    @Test
    fun sequenceRequiresSequenceNode() {
        val editor = engine.parse(StringYamlInput("value: {}" )).editor()
        assertFailsWith<YamlEditException> {
            editor.sequence(YamlPath.root().child("value"))
        }
    }

    @Test
    fun collectionEditorLookupRequiresExistingPath() {
        val editor = engine.parse(StringYamlInput("{}" )).editor()
        val exception = assertFailsWith<YamlEditException> {
            editor.mapping(YamlPath.root().child("missing"))
        }
        assertEquals(YamlPath.root().child("missing"), exception.path)
    }
}
