package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlin.test.*

class SnakeYamlMinimalReductionPresentationTest {
    @Serializable private data class Database(val host: String = "localhost", val port: Int = 3306)
    @Serializable private data class Item(val name: String, val port: Int = 25565)
    @Serializable private data class Config(val token: String, val base: Int = 10, val derived: Int = base + 1,
        val database: Database = Database(host = "prod"), val items: List<Item> = emptyList(),
        val values: Map<Int, Item> = emptyMap())
    private val engine = SnakeYamlEngine()

    @Test fun protectedParentKeepsUnknownDataAndSurvivingRawValues() {
        val source = MemoryYamlSource("token: 't' # token\ndatabase:\n  host: 'prod' # host\n  port: 3306\n  plugin: keep # plugin\n")
        val config = yamlConfig<Config>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.save()
        assertEquals(session.value, config.load().value)
        val root = assertIs<YamlMappingNode>(engine.parse(source).root)
        val database = assertIs<YamlMappingNode>(root["database"])
        assertNotNull(database["plugin"])
        assertNull(database["port"])
        assertTrue(source.text.contains("'prod' # host"), source.text)
        assertTrue(source.text.contains("keep # plugin"), source.text)
        val text = source.text
        val writes = source.commitCount
        session.save()
        assertEquals(text, source.text)
        assertEquals(writes, source.commitCount)
    }

    @Test fun dependentSiblingIsRetainedWhenItsParentInputChanges() {
        val source = MemoryYamlSource("token: t\nbase: 100\nderived: +101 # derived\nfuture: keep # future\n")
        val config = yamlConfig<Config>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = session.value.copy(base = 10)
        session.save()
        assertEquals(10, config.load().value.base)
        assertEquals(101, config.load().value.derived)
        val root = assertIs<YamlMappingNode>(engine.parse(source).root)
        assertNull(root["base"])
        assertTrue(source.text.contains("+101 # derived"), source.text)
        assertTrue(source.text.contains("keep # future"), source.text)
    }

    @Test fun emptyChildMustRemainWhenRemovingItWouldSelectADifferentParentDefault() {
        val source = MemoryYamlSource("token: t\ndatabase:\n  host: localhost\n  port: 3306\n")
        val config = yamlConfig<Config>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.save()
        assertEquals(Database(), config.load().value.database)
        val root = assertIs<YamlMappingNode>(engine.parse(source).root)
        assertEquals(0, assertIs<YamlMappingNode>(root["database"]).size)
    }

    @Test fun unknownEntriesInsideCollectionsPreventWholeParentDeletion() {
        val source = MemoryYamlSource("token: t\nitems:\n- name: a\n  plugin: list-data # list\nvalues:\n  0xff:\n    name: b\n    plugin: map-data # map\n")
        val defaults = StringYamlInput("items:\n- name: a\nvalues:\n  255:\n    name: b\n")
        val config = yamlConfig<Config>(engine, source, defaultsSource = defaults, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.save()
        assertEquals(session.value, config.load().value)
        val root = assertIs<YamlMappingNode>(engine.parse(source).root)
        assertNotNull(root["items"])
        assertNotNull(root["values"])
        assertTrue(source.text.contains("list-data # list"), source.text)
        assertTrue(source.text.contains("map-data # map"), source.text)
        assertTrue(source.text.contains("0xff:"), source.text)
    }

    @Test fun explicitEmptyOverridesAreNotRemovedAgainstNonemptyDefaults() {
        val source = MemoryYamlSource("token: t\nitems: []\nvalues: {}\n")
        val defaults = StringYamlInput("items:\n- name: a\nvalues:\n  1:\n    name: b\n")
        val config = yamlConfig<Config>(engine, source, defaultsSource = defaults, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.save()
        assertEquals(emptyList(), config.load().value.items)
        assertEquals(emptyMap(), config.load().value.values)
        val root = assertIs<YamlMappingNode>(engine.parse(source).root)
        assertNotNull(root["items"])
        assertNotNull(root["values"])
    }

    @Test fun preserveModeDoesNotStartRemovingRedundantOverrides() {
        val source = MemoryYamlSource("token: t\nbase: +10 # redundant\n")
        val config = yamlConfig<Config>(engine, source, saveMode = YamlSaveMode.PRESERVE_OVERRIDES)
        val session = config.load()
        session.value = session.value.copy(token = "changed")
        session.save()
        assertEquals(session.value, config.load().value)
        assertTrue(source.text.contains("+10 # redundant"), source.text)
    }
}
