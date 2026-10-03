package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SnakeYamlIdentityAmbiguityTest {
    @Serializable private data class NullableServer(val name: String?, val port: Int = 1)

    @Test fun identityChangeAtSamePositionDoesNotTransferUnknownDataOrComment() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  port: 1 # old-owner\n  plugin-option: old-only\n")
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(IdentityServer("b", 1))
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals(null, identityItems(source).single().text("plugin-option"))
            assertFalse(source.text.contains("old-owner"), source.text)
        }
    }

    @Test fun duplicateOldIdentityDoesNotChooseOneChangedElementAsOwner() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: first\n- name: a\n  port: 2\n  extra: second\n")
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(IdentityServer("a", 3))
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals(null, identityItems(source).single().text("extra"))
        }
    }

    @Test fun duplicateNewIdentityDoesNotCloneOldMetadataIntoEitherChangedElement() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: old-only\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("a", 2), IdentityServer("a", 3))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf(null, null), identityItems(source).map { it.text("extra") })
    }

    @Test fun exactValueMatchRemainsAvailableForDuplicateIdentity() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: first\n- name: a\n  port: 2\n  extra: second\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("a", 2))
        session.save()
        assertEquals("second", identityItems(source).single().text("extra"))
        assertEquals(session.value, config.load().value)
    }

    @Test fun duplicatesDoNotDisableOtherUniqueIdentities() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: first\n- name: a\n  port: 2\n  extra: second\n- name: b\n  port: 3\n  extra: unique\n")
        val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(IdentityServer("b", 30), IdentityServer("a", 1), IdentityServer("a", 2))
        session.save()
        assertEquals(listOf("unique", "first", "second"), identityItems(source).map { it.text("extra") })
        assertEquals(session.value, config.load().value)
    }

    @Test fun nullIdentityDoesNotUseTheSecondRequiredOrOptionalFieldAsIdentity() {
        val source = MemoryYamlSource("- name: null\n  port: 1\n  extra: old-only\n")
        val config = yamlConfig<List<NullableServer>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(NullableServer(null, 2))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(null, identityItems(source).single().text("extra"))
    }
}
