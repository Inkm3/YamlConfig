package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnakeYamlStructuralMinimalTest {
    private val engine = SnakeYamlEngine()
    private val mode = YamlSaveMode.MINIMAL_DIFFERENCE

    @Test fun defaultMatchingListIsRemovedAsAWholeAfterStructuralPlanning() {
        val source = MemoryYamlSource("- a # a\n- custom # custom\n")
        val config = yamlConfig<List<String>>(engine, source,
            defaultsSource = StringYamlInput("- a\n- b\n"), saveMode = mode)
        val session = config.load()
        session.value = listOf("a", "b")
        session.save()
        assertNull(engine.parse(source).root)
        assertEquals(listOf("a", "b"), config.load().value)
    }

    @Test fun unknownInsideDefaultMatchingObjectListPreventsParentRemoval() {
        val source = MemoryYamlSource("- name: a\n  port: 30000 # port\n  plugin: keep # plugin\n")
        val defaults = StringYamlInput("- name: a\n  port: 25565\n")
        val config = yamlConfig<List<CollectionServer>>(engine, source, defaultsSource = defaults, saveMode = mode)
        val session = config.load()
        session.value = listOf(CollectionServer("a"))
        session.save()
        assertEquals(session.value, config.load().value)
        assertIs<YamlSequenceNode>(engine.parse(source).root)
        assertTrue(source.text.contains("25565 # port"), source.text)
        assertTrue(source.text.contains("keep # plugin"), source.text)
    }

    @Test fun unknownInsideDefaultMatchingMapValuePreventsParentRemoval() {
        val source = MemoryYamlSource("entry:\n  name: a\n  port: 30000\n  plugin: keep # plugin\n")
        val defaults = StringYamlInput("entry:\n  name: a\n  port: 25565\n")
        val config = yamlConfig<Map<String, CollectionServer>>(engine, source, defaultsSource = defaults, saveMode = mode)
        val session = config.load()
        session.value = mapOf("entry" to CollectionServer("a"))
        session.save()
        assertEquals(session.value, config.load().value)
        assertIs<YamlMappingNode>(engine.parse(source).root)
        assertTrue(source.text.contains("keep # plugin"), source.text)
    }

    @Test fun emptyCollectionsRemainExplicitAgainstNonemptyYamlDefaults() {
        val listSource = MemoryYamlSource("- user # user\n")
        val listConfig = yamlConfig<List<String>>(engine, listSource,
            defaultsSource = StringYamlInput("- default\n"), saveMode = mode)
        val listSession = listConfig.load()
        listSession.value = emptyList()
        listSession.save()
        assertEquals(emptyList(), listConfig.load().value)
        assertEquals(0, assertIs<YamlSequenceNode>(engine.parse(listSource).root).size)

        val mapSource = MemoryYamlSource("user: 2 # user\n")
        val mapConfig = yamlConfig<Map<String, Int>>(engine, mapSource,
            defaultsSource = StringYamlInput("default: 1\n"), saveMode = mode)
        val mapSession = mapConfig.load()
        mapSession.value = emptyMap()
        mapSession.save()
        assertEquals(emptyMap(), mapConfig.load().value)
        assertEquals(0, assertIs<YamlMappingNode>(engine.parse(mapSource).root).size)
    }
}
