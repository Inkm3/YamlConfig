package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SnakeYamlStructuralMapTest {
    private val engine = SnakeYamlEngine()

    @Test fun rawIntegerKeyAndValueCommentSurviveUpdate() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("0xff: 'old' # existing\n2: keep # untouched\n")
            val config = yamlConfig<Map<Int, String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = linkedMapOf(255 to "new", 2 to "keep")
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("0xff: 'new' # existing"), source.text)
            assertTrue(source.text.contains("2: keep # untouched"), source.text)
        }
    }

    @Test fun updatingObjectValueKeepsRawKeyUnknownChildAndPortComment() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("0xff:\n  name: a\n  port: +25565 # port-comment\n  plugin: keep # plugin-comment\n")
            val config = yamlConfig<Map<Int, CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = mapOf(255 to CollectionServer("a", 30000))
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("0xff:"), source.text)
            assertTrue(source.text.contains("30000 # port-comment"), source.text)
            val root = assertIs<YamlMappingNode>(engine.parse(source).root)
            val value = assertIs<YamlMappingNode>(root[YamlMapKey("0xff", YamlScalarKind.INTEGER)])
            assertEquals("keep", assertIs<YamlScalarNode>(value["plugin"]).value)
            assertTrue(source.text.contains("# plugin-comment"), source.text)
        }
    }

    @Test fun insertingAndRemovingKeysDoesNotRebuildSurvivingEntries() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("0x01: 'keep' # keep-comment\n2: drop # drop-comment\n")
            val config = yamlConfig<Map<Int, String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = linkedMapOf(1 to "keep", 3 to "new")
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("0x01: 'keep' # keep-comment"), source.text)
            assertFalse(source.text.contains("# drop-comment"), source.text)
        }
    }

    @Test fun mapValueListMovesKeepTheirElementPresentation() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("group:\n- 'a' # a-comment\n- b # b-comment\n")
            val config = yamlConfig<Map<String, List<String>>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = mapOf("group" to listOf("b", "a"))
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("'a' # a-comment"), source.text)
            assertTrue(source.text.indexOf("# b-comment") < source.text.indexOf("# a-comment"), source.text)
        }
    }

    @Test fun integerSpellingOfFloatingPointKeyIsRetained() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("1: old # numeric-key\n")
            val config = yamlConfig<Map<Double, String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = mapOf(1.0 to "new")
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("1: new # numeric-key"), source.text)
            assertFalse(source.text.contains("1.0:"), source.text)
        }
    }

    @Test fun nullAndBooleanKeySpellingsAreRetained() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("~: old-null # null-key\nTRUE: old-true # bool-key\n")
            val config = yamlConfig<Map<Boolean?, String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = linkedMapOf(null to "new-null", true to "new-true")
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("~: new-null # null-key"), source.text)
            assertTrue(source.text.contains("TRUE: new-true # bool-key"), source.text)
        }
    }

    @Test fun changingOnlyMapIterationOrderDoesNotRewriteYaml() {
        for (mode in YamlSaveMode.entries) {
            val original = "a: 1 # a\nb: 2 # b\n"
            val source = MemoryYamlSource(original)
            val session = yamlConfig<Map<String, Int>>(engine, source, saveMode = mode).load()
            session.value = linkedMapOf("b" to 2, "a" to 1)
            session.save()
            assertEquals(original, source.text)
            assertEquals(0, source.commitCount)
        }
    }

    @Test fun renamedMapEntryDoesNotReceiveTheRemovedEntriesUnknownData() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("old:\n  name: a\n  plugin: old-only\n")
            val config = yamlConfig<Map<String, CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = mapOf("new" to session.value.getValue("old"))
            session.save()
            assertEquals(session.value, config.load().value)
            assertFalse(source.text.contains("old-only"), source.text)
        }
    }
}
