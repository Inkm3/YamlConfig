package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SnakeYamlIdentityNestedDefaultsTest {
    @Serializable private data class Group(val name: String, val servers: List<IdentityServer>)
    @Serializable private data class Dependent(val name: String, val base: Int = 10, val derived: Int = base + 1)

    @Test fun parentAndChildCanMoveAndChangeInTheSameSave() {
        val source = MemoryYamlSource("""
            - name: first
              extra: parent-first
              servers:
                - name: a
                  port: 1
                  extra: A
            - name: second
              extra: parent-second
              servers:
                - name: b
                  port: 2
                  extra: B
                - name: c
                  port: 3
                  extra: C
        """.trimIndent())
        val config = yamlConfig<List<Group>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(session.value[1].copy(servers = listOf(IdentityServer("c", 30), IdentityServer("b", 20))),
            session.value[0])
        session.save()
        assertEquals(session.value, config.load().value)
        val groups = identityItems(source)
        assertEquals(listOf("parent-second", "parent-first"), groups.map { it.text("extra") })
        val children = assertIs<YamlSequenceNode>(groups[0]["servers"]).elements.map { assertIs<YamlMappingNode>(it) }
        assertEquals(listOf("C", "B"), children.map { it.text("extra") })
    }

    @Test fun identityMatchingWorksInsideMapValuesWithoutChangingMapSemantics() {
        val source = MemoryYamlSource("primary:\n- name: a\n  port: 1\n  extra: A\n- name: b\n  port: 2\n  extra: B\n")
        val config = yamlConfig<Map<String, List<IdentityServer>>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = mapOf("primary" to listOf(IdentityServer("b", 20), IdentityServer("a", 10)))
        session.save()
        assertEquals(session.value, config.load().value)
        val root = assertIs<YamlMappingNode>(SnakeYamlEngine().parse(source).root)
        val items = assertIs<YamlSequenceNode>(root["primary"]).elements.map { assertIs<YamlMappingNode>(it) }
        assertEquals(listOf("B", "A"), items.map { it.text("extra") })
    }

    @Test fun defaultDependencyRepairReachesMovedIdentityMatchedObject() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  extra: A\n- name: b\n  base: 100\n  extra: B\n")
            val config = yamlConfig<List<Dependent>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(session.value[1].copy(base = 10), session.value[0])
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals(101, config.load().value[0].derived)
            assertEquals(listOf("B", "A"), identityItems(source).map { it.text("extra") })
        }
    }

    @Test fun inheritedListIsWrittenWithoutPretendingDefaultNodesAreUserNodes() {
        for (mode in YamlSaveMode.entries) {
            val defaults = StringYamlInput("- name: a\n  port: 1 # default-a\n- name: b\n  port: 2 # default-b\n")
            val source = MemoryYamlSource("")
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = listOf(IdentityServer("b", 20), IdentityServer("a", 1))
            session.save()
            assertEquals(session.value, config.load().value)
            assertFalse(source.text.contains("# default-"), source.text)
        }
    }

    @Test fun minimalCanRemoveWholeOverrideAfterIdentityMoveAndUpdate() {
        val defaults = StringYamlInput("- name: b\n  port: 3\n- name: a\n  port: 1\n")
        val source = MemoryYamlSource("- name: a\n  port: 1\n- name: b\n  port: 2\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, defaultsSource = defaults,
            saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = listOf(IdentityServer("b", 3), IdentityServer("a", 1))
        session.save()
        assertNull(SnakeYamlEngine().parse(source).root)
        assertEquals(session.value, config.load().value)
    }

    @Test fun minimalKeepsUnknownDataEvenWhenListMatchesYamlDefault() {
        val defaults = StringYamlInput("- name: b\n  port: 3\n- name: a\n  port: 1\n")
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: A\n- name: b\n  port: 2\n  extra: B\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, defaultsSource = defaults,
            saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = listOf(IdentityServer("b", 3), IdentityServer("a", 1))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf("B", "A"), identityItems(source).map { it.text("extra") })
    }
}
