package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.schema.*
import com.github.inkm3.yamlconfig.testsupport.*
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.*

class StructuralSaveTest {
    class MapConfig {
        var values: Map<Int, String> = linkedMapOf()
    }

    class ListConfig {
        var values: List<String> = emptyList()
    }

    private val mapSchema = yamlObject(::MapConfig) {
        field(MapConfig::values, map(int(), str()))
    }

    private val listSchema = yamlObject(::ListConfig) {
        field(ListConfig::values, list(str()))
    }

    @Test fun mapSaveUsesStructuralInsertRemoveAndPreservesExistingLexicalKey() {
        val userMap = YamlMappingNode(linkedMapOf(
            YamlMapKey("0xff", YamlScalarKind.INTEGER) to s("old"),
            YamlMapKey("2", YamlScalarKind.INTEGER) to s("remove"),
        ))
        val source = TestYamlSource(stringMappingOf("values" to userMap))
        val engine = TestYamlEngine()
        val session = yamlConfig(engine, source, mapSchema).load()

        session.value.values = LinkedHashMap(session.value.values).apply {
            this[255] = "new"
            remove(2)
            this[3] = "added"
        }
        session.save()

        val root = assertIs<YamlMappingNode>(source.rootNode)
        val values = assertIs<YamlMappingNode>(root["values"])
        assertEquals(s("new"), values[YamlMapKey("0xff", YamlScalarKind.INTEGER)])
        assertFalse(values.containsKey(YamlMapKey("2", YamlScalarKind.INTEGER)))
        assertEquals(s("added"), values[YamlMapKey("3", YamlScalarKind.INTEGER)])
        assertTrue(source.lastOperations.any { it is TestEditOperation.MappingRemove })
        assertTrue(source.lastOperations.any { it is TestEditOperation.MappingInsert })
    }

    @Test fun listSaveUsesMoveInsertRemoveWithoutReplacingWholeSequence() {
        val source = TestYamlSource(stringMappingOf(
            "values" to sequenceOf(s("A"), s("B"), s("C")),
        ))
        val session = yamlConfig(TestYamlEngine(), source, listSchema).load()

        session.value.values = listOf("C", "A", "X")
        session.save()

        val values = assertIs<YamlSequenceNode>(assertIs<YamlMappingNode>(source.rootNode)["values"])
        assertEquals(listOf(s("C"), s("A"), s("X")), values.elements)
        assertTrue(source.lastOperations.any { it is TestEditOperation.SequenceMove })
        assertTrue(source.lastOperations.any { it is TestEditOperation.SequenceInsert || it is TestEditOperation.SequenceReplace || it is TestEditOperation.Set })
        assertFalse(source.lastOperations.any { it is TestEditOperation.Set && it.path.toString() == "$.values" })
    }

    @Test fun collectionInheritedOnlyFromDefaultsIsWrittenAsWholeOverrideWhenChanged() {
        val defaults = TestYamlSource(stringMappingOf(
            "values" to sequenceOf(s("A"), s("B")),
        ))
        val user = TestYamlSource(stringMappingOf())
        val session = yamlConfig(TestYamlEngine(), user, listSchema, defaults).load()
        session.value.values = listOf("A", "X")
        session.save()

        assertEquals(
            sequenceOf(s("A"), s("X")),
            assertIs<YamlMappingNode>(user.rootNode)["values"],
        )
        assertTrue(user.lastOperations.any { it is TestEditOperation.Set && it.path.toString() == "$.values" })
    }

    @Test fun minimalDifferenceRemovesWholeCollectionEqualToDefault() {
        val defaults = TestYamlSource(stringMappingOf(
            "values" to sequenceOf(s("A"), s("B")),
        ))
        val user = TestYamlSource(stringMappingOf(
            "values" to sequenceOf(s("X")),
        ))
        val session = yamlConfig(TestYamlEngine(), user, listSchema, defaults, YamlSaveMode.MINIMAL_DIFFERENCE).load()
        session.value.values = listOf("A", "B")
        session.save()

        assertFalse(assertIs<YamlMappingNode>(user.rootNode).containsKey("values"))
    }
}
