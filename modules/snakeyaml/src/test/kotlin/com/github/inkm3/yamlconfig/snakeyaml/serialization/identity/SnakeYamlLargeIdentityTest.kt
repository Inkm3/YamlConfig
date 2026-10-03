package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SnakeYamlLargeIdentityTest {
    @Serializable private data class Entry(val name: String, val port: Int = 25565)
    @Serializable private data class Config(val items: List<Entry>)
    private val engine = SnakeYamlEngine()

    private fun source(count: Int = 140): MemoryYamlSource = MemoryYamlSource(buildString {
        append("items:\n")
        repeat(count) { index ->
            append("- name: 'item-$index'\n  port: +25565 # comment-$index\n  owner: item-$index\n")
        }
    })

    @Test fun indexedReverseAndUpdateKeepUnknownOwnershipInBothModes() {
        for (mode in YamlSaveMode.entries) {
            val source = source()
            val config = yamlConfig<Config>(engine, source, saveMode = mode)
            val session = config.load()
            val expected = session.value.copy(items = session.value.items.reversed().mapIndexed { index, item ->
                if (index == 0) item.copy(port = 30000) else item
            })
            session.value = expected
            session.save()
            assertEquals(expected, config.load().value)
            val root = assertIs<YamlMappingNode>(engine.parse(source).root)
            val items = assertIs<YamlSequenceNode>(root["items"])
            for (node in items.elements) {
                val item = assertIs<YamlMappingNode>(node)
                assertEquals(assertIs<YamlScalarNode>(item["name"]).value,
                    assertIs<YamlScalarNode>(item["owner"]).value)
            }
            assertTrue(source.text.contains("# comment-139"), source.text)
            if (mode == YamlSaveMode.PRESERVE_OVERRIDES) {
                assertTrue(source.text.contains("+25565 # comment-0"), source.text)
            }
        }
    }

    @Test fun insertRemoveAndMoveDoNotTransferDeletedUnknownData() {
        val source = source()
        val config = yamlConfig<Config>(engine, source)
        val session = config.load()
        val expected = session.value.copy(items = listOf(Entry("new", 30000)) +
            session.value.items.drop(1).reversed())
        session.value = expected
        session.save()
        assertEquals(expected, config.load().value)
        val nodes = assertIs<YamlSequenceNode>(assertIs<YamlMappingNode>(engine.parse(source).root)["items"])
        assertFalse(assertIs<YamlMappingNode>(nodes[0]).containsKey("owner"))
        for (node in nodes.elements.drop(1)) {
            val item = assertIs<YamlMappingNode>(node)
            assertEquals(assertIs<YamlScalarNode>(item["name"]).value,
                assertIs<YamlScalarNode>(item["owner"]).value)
        }
    }

    @Test fun thresholdBoundariesRetainTheSameValueContract() {
        for (count in listOf(127, 128, 129)) {
            val source = source(count)
            val config = yamlConfig<Config>(engine, source)
            val session = config.load()
            val expected = session.value.copy(items = session.value.items.reversed())
            session.value = expected
            session.save()
            assertEquals(expected, config.load().value)
            val before = source.text
            session.save()
            assertEquals(before, source.text)
        }
    }
}
