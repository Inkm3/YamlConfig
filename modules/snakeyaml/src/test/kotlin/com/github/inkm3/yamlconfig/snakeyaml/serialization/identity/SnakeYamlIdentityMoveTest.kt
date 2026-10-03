package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnakeYamlIdentityMoveTest {
    @Test fun movingAndUpdatingCreativeRetainsItsOwnCommentsAndUnknownKey() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("""
                - name: lobby
                  port: +25565 # lobby port comment
                  plugin-option: lobby-only
                - name: creative
                  port: +25567 # creative port comment
                  # creative plugin option
                  plugin-option: keep-me
            """.trimIndent())
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(session.value[1].copy(port = 30000), session.value[0])
            val expected = session.value
            session.save()
            assertEquals(expected, config.load().value)
            val items = identityItems(source)
            assertEquals(listOf("creative", "lobby"), items.map { it.text("name") })
            assertEquals(listOf("keep-me", "lobby-only"), items.map { it.text("plugin-option") })
            assertTrue(source.text.contains("port: 30000 # creative port comment"), source.text)
            assertTrue(source.text.contains("port: +25565 # lobby port comment"), source.text)
            assertTrue(source.text.contains("# creative plugin option"), source.text)
            val saved = source.text
            session.save()
            assertEquals(saved, source.text)
            assertEquals(1, source.commitCount)
        }
    }

    @Test fun allChangedItemsReorderWithoutExchangingUnknownData() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: A\n- name: b\n  port: 2\n  extra: B\n- name: c\n  port: 3\n  extra: C\n")
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(IdentityServer("c", 30), IdentityServer("a", 10), IdentityServer("b", 20))
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals(listOf("C", "A", "B"), identityItems(source).map { it.text("extra") })
        }
    }

    @Test fun insertingNewItemDoesNotConsumeMovedAndChangedOldItem() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: A\n- name: b\n  port: 2\n  extra: B\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("x", 9), IdentityServer("b", 20), IdentityServer("a", 10))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf(null, "B", "A"), identityItems(source).map { it.text("extra") })
    }

    @Test fun successiveSavesUseTheLastSuccessfulIdentityPositions() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: A\n- name: b\n  port: 2\n  extra: B\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("b", 20), IdentityServer("a", 10))
        session.save()
        session.value = listOf(IdentityServer("a", 11), IdentityServer("b", 21))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf("A", "B"), identityItems(source).map { it.text("extra") })
        session.save()
        assertEquals(2, source.commitCount)
    }

    @Test fun movingElementKeepsItsQuotedNameAndUntouchedScalarLexeme() {
        val source = MemoryYamlSource("- name: 'a'\n  port: 0x01 # a port\n  extra: A\n- name: 'b'\n  port: +2 # b port\n  extra: B\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("b", 3), IdentityServer("a", 1))
        session.save()
        assertTrue(source.text.contains("name: 'b'"), source.text)
        assertTrue(source.text.contains("port: 3 # b port"), source.text)
        assertTrue(source.text.contains("port: 0x01 # a port"), source.text)
        assertEquals(session.value, config.load().value)
    }
}
