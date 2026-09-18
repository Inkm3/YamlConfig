package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SnakeYamlCollectionEditorTest {
    private val engine = SnakeYamlEngine()
    private val stringA = YamlScalarNode("a", YamlScalarKind.STRING)
    private val stringB = YamlScalarNode("b", YamlScalarKind.STRING)
    private val stringX = YamlScalarNode("x", YamlScalarKind.STRING)

    @Test
    fun mappingInsertAddsNewEntry() {
        val editor = engine.parse(StringYamlInput("{}" )).editor()
        editor.mapping(YamlPath.root()).insert(YamlMapKey("name"), stringA)

        val root = assertIs<YamlMappingNode>(editor.root)
        assertEquals(stringA, root["name"])
    }

    @Test
    fun mappingInsertRejectsExistingEntry() {
        val editor = engine.parse(StringYamlInput("name: old")).editor()
        assertFailsWith<YamlEditException> {
            editor.mapping(YamlPath.root()).insert(YamlMapKey("name"), stringA)
        }
    }

    @Test
    fun mappingReplaceRequiresExistingEntry() {
        val editor = engine.parse(StringYamlInput("{}" )).editor()
        assertFailsWith<YamlEditException> {
            editor.mapping(YamlPath.root()).replace(YamlMapKey("missing"), stringA)
        }
    }

    @Test
    fun mappingReplaceChangesOnlyValue() {
        val editor = engine.parse(StringYamlInput("name: old\nother: keep")).editor()
        editor.mapping(YamlPath.root()).replace(YamlMapKey("name"), stringX)

        val root = assertIs<YamlMappingNode>(editor.root)
        assertEquals(stringX, root["name"])
        assertEquals(YamlScalarNode("keep", YamlScalarKind.STRING), root["other"])
    }

    @Test
    fun mappingRemoveMissingEntryIsNoOp() {
        val editor = engine.parse(StringYamlInput("name: old")).editor()
        val before = editor.root
        editor.mapping(YamlPath.root()).remove(YamlMapKey("missing"))
        assertEquals(before, editor.root)
    }

    @Test
    fun sequenceInsertSupportsBeginningMiddleAndEnd() {
        val editor = engine.parse(StringYamlInput("items: [a, b]")).editor()
        val path = YamlPath.root().child("items")
        val sequence = editor.sequence(path)

        sequence.insert(0, stringX)
        sequence.insert(2, YamlScalarNode("m", YamlScalarKind.STRING))
        sequence.insert(sequence.size, YamlScalarNode("z", YamlScalarKind.STRING))

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(listOf("x", "a", "m", "b", "z"), items.elements.map { (it as YamlScalarNode).value })
    }

    @Test
    fun sequenceInsertRejectsNegativeAndTooLargeIndices() {
        val editor = engine.parse(StringYamlInput("items: [a]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))

        assertFailsWith<YamlEditException> { sequence.insert(-1, stringX) }
        assertFailsWith<YamlEditException> { sequence.insert(2, stringX) }
    }

    @Test
    fun sequenceReplaceRequiresExistingElement() {
        val editor = engine.parse(StringYamlInput("items: [a]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))

        sequence.replace(0, stringX)
        assertFailsWith<YamlEditException> { sequence.replace(1, stringX) }

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(stringX, items[0])
    }

    @Test
    fun sequenceRemoveMissingElementIsNoOp() {
        val editor = engine.parse(StringYamlInput("items: [a]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))
        sequence.remove(-1)
        sequence.remove(4)

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(listOf(stringA), items.elements)
    }

    @Test
    fun sequenceMoveUsesFinalDestinationIndex() {
        val editor = engine.parse(StringYamlInput("items: [a, b, c]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))

        sequence.move(0, 2)
        var root = assertIs<YamlMappingNode>(editor.root)
        var items = assertIs<YamlSequenceNode>(root["items"])
        assertEquals(listOf("b", "c", "a"), items.elements.map { (it as YamlScalarNode).value })

        sequence.move(2, 0)
        root = assertIs(editor.root)
        items = assertIs(root["items"])
        assertEquals(listOf("a", "b", "c"), items.elements.map { (it as YamlScalarNode).value })
    }

    @Test
    fun sequenceMoveSameIndexIsNoOp() {
        val editor = engine.parse(StringYamlInput("items: [a, b]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))
        val before = editor.root
        sequence.move(1, 1)
        assertEquals(before, editor.root)
    }

    @Test
    fun sequenceMoveRejectsInvalidIndices() {
        val editor = engine.parse(StringYamlInput("items: [a, b]")).editor()
        val sequence = editor.sequence(YamlPath.root().child("items"))
        assertFailsWith<YamlEditException> { sequence.move(-1, 0) }
        assertFailsWith<YamlEditException> { sequence.move(0, 2) }
    }

    @Test
    fun collectionEditorsArePathBackedInsteadOfHoldingStaleNodes() {
        val editor = engine.parse(StringYamlInput("items: [a]\nmap: {a: old}")).editor()
        val sequencePath = YamlPath.root().child("items")
        val mappingPath = YamlPath.root().child("map")
        val sequenceEditor = editor.sequence(sequencePath)
        val mappingEditor = editor.mapping(mappingPath)

        editor.set(sequencePath, YamlSequenceNode(listOf(stringB)))
        editor.set(
            mappingPath,
            YamlMappingNode(linkedMapOf(YamlMapKey("b") to stringB)),
        )

        sequenceEditor.insert(1, stringX)
        mappingEditor.insert(YamlMapKey("c"), stringX)

        val root = assertIs<YamlMappingNode>(editor.root)
        val items = assertIs<YamlSequenceNode>(root["items"])
        val map = assertIs<YamlMappingNode>(root["map"])
        assertEquals(listOf("b", "x"), items.elements.map { (it as YamlScalarNode).value })
        assertEquals(stringB, map["b"])
        assertEquals(stringX, map["c"])
    }
}
