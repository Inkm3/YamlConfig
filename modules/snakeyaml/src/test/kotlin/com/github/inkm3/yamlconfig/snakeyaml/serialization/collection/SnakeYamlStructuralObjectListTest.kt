package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SnakeYamlStructuralObjectListTest {
    private val engine = SnakeYamlEngine()
    private val yaml = """
        - name: a
          port: +25565 # a-port
          plugin: a-data # a-plugin
        - name: b
          port: 25566 # b-port
          plugin: b-data # b-plugin
    """.trimIndent()

    private fun plugin(source: MemoryYamlSource, index: Int): String {
        val sequence = assertIs<YamlSequenceNode>(engine.parse(source).root)
        return assertIs<YamlScalarNode>(assertIs<YamlMappingNode>(sequence[index])["plugin"]).value
    }

    @Test fun movedObjectKeepsItsUnknownKeysAndScalarLexeme() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource(yaml)
            val config = yamlConfig<List<CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = session.value.reversed()
            session.save()
            assertEquals(listOf(CollectionServer("b", 25566), CollectionServer("a")), config.load().value)
            assertEquals("b-data", plugin(source, 0))
            assertEquals("a-data", plugin(source, 1))
            assertTrue(source.text.contains("+25565 # a-port"), source.text)
            assertTrue(source.text.contains("25566 # b-port"), source.text)
        }
    }

    @Test fun portUpdateKeepsNestedCommentAndUnknownEntry() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource(yaml)
            val config = yamlConfig<List<CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = session.value.mapIndexed { index, value -> if (index == 0) value.copy(port = 30000) else value }
            session.save()
            assertEquals(listOf(CollectionServer("a", 30000), CollectionServer("b", 25566)), config.load().value)
            assertTrue(source.text.contains("30000 # a-port"), source.text)
            assertTrue(source.text.contains("25566 # b-port"), source.text)
            assertEquals("a-data", plugin(source, 0))
            assertEquals("b-data", plugin(source, 1))
            assertTrue(source.text.contains("# a-plugin"), source.text)
        }
    }

    @Test fun exactMoveThenUpdateOfAnotherElementUsesTheNewIndex() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource(yaml + "\n- name: drop\n  port: 25567 # drop-port\n")
            val config = yamlConfig<List<CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf(session.value[1], session.value[0].copy(port = 30000))
            session.save()
            assertEquals(listOf(CollectionServer("b", 25566), CollectionServer("a", 30000)), config.load().value)
            assertEquals("b-data", plugin(source, 0))
            assertEquals("a-data", plugin(source, 1))
            assertTrue(source.text.contains("30000 # a-port"), source.text)
            assertTrue(source.text.contains("25566 # b-port"), source.text)
            assertFalse(source.text.contains("# drop-port"), source.text)
        }
    }

    @Test fun recursiveListInsideObjectUsesStructuralEditsToo() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  ports:\n    - +1 # one\n    - 0x02 # two\n  plugin: keep\n")
            val config = yamlConfig<List<CollectionGroup>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf(CollectionGroup("a", listOf(2, 1, 3)))
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("0x02 # two"), source.text)
            assertTrue(source.text.contains("+1 # one"), source.text)
            assertEquals("keep", plugin(source, 0))
        }
    }

    @Test fun duplicateEqualObjectsDoNotMixTheirUnknownData() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  plugin: first\n- name: a\n  plugin: second\n")
            val config = yamlConfig<List<CollectionServer>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = session.value.take(1)
            session.save()
            assertEquals(listOf(CollectionServer("a")), config.load().value)
            assertEquals("first", plugin(source, 0))
            assertFalse(source.text.contains("second"), source.text)
        }
    }

    @Test fun dependentDefaultRepairInsideListPreservesUnknownData() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- base: 100 # base-comment\n  plugin: keep\n")
            val config = yamlConfig<List<CollectionDependent>>(engine, source, saveMode = mode)
            val session = config.load()
            assertEquals(listOf(CollectionDependent(100, 101)), session.value)
            session.value = listOf(CollectionDependent(10, 101))
            session.save()
            assertEquals(listOf(CollectionDependent(10, 101)), config.load().value)
            assertTrue(source.text.contains("10 # base-comment"), source.text)
            assertEquals("keep", plugin(source, 0))
        }
    }

    @Test fun missingNestedParentIsMaterializedWithoutReplacingTheListElement() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a # name-comment\n  plugin: keep\n")
            val config = yamlConfig<List<CollectionParent>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf(session.value.single().copy(database = CollectionDatabase("prod", 5432)))
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals("prod", config.load().value.single().database.host)
            assertEquals("keep", plugin(source, 0))
            assertTrue(source.text.contains("a # name-comment"), source.text)
        }
    }
}
