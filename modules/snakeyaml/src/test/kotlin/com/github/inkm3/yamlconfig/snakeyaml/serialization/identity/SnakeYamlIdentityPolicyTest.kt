package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.annotation.YamlIdentity
import com.github.inkm3.yamlconfig.annotation.YamlIdentityDisabled
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SnakeYamlIdentityPolicyTest {
    @Serializable private data class Explicit(val name: String, @YamlIdentity @SerialName("entry-id") val id: Int = 0)
    @Serializable @YamlIdentityDisabled private data class Disabled(val name: String, val port: Int = 1)
    @Serializable private data class Optional(val name: String = "a", val port: Int = 1)
    @Serializable private data class Conflicting(@YamlIdentity val a: String, @YamlIdentity val b: String)
    @Serializable private data class Hidden(@YamlIdentity @EncodeDefault(EncodeDefault.Mode.NEVER) val id: Int = 1,
        val value: Int = 1)
    @Serializable private data class Numeric(val id: Int, val port: Int = 1)

    @Test fun explicitOptionalIdentityUsesSerialNameAndAllowsNameChange() {
        val source = MemoryYamlSource("- name: a\n  entry-id: 1\n  extra: A\n- name: b\n  entry-id: 2\n  extra: B\n")
        val config = yamlConfig<List<Explicit>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(Explicit("renamed-b", 2), Explicit("a", 1))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf("B", "A"), identityItems(source).map { it.text("extra") })
    }

    @Test fun disabledConventionKeepsTheDocumentedPositionalBehavior() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: positional\n")
        val config = yamlConfig<List<Disabled>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(Disabled("b", 2))
        session.save()
        assertEquals("positional", identityItems(source).single().text("extra"))
        assertEquals(session.value, config.load().value)
    }

    @Test fun allOptionalClassHasNoImplicitIdentity() {
        val source = MemoryYamlSource("- name: a\n  port: 1\n  extra: positional\n")
        val config = yamlConfig<List<Optional>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(Optional("b", 2))
        session.save()
        assertEquals("positional", identityItems(source).single().text("extra"))
        assertEquals(session.value, config.load().value)
    }

    @Test fun ambiguousAnnotationConfigurationFailsBeforeWritingAndKeepsBaseline() {
        val original = "- a: first\n  b: second\n"
        val source = MemoryYamlSource(original)
        val session = yamlConfig<List<Conflicting>>(SnakeYamlEngine(), source).load()
        val before = session.value
        session.value = listOf(Conflicting("changed", "second"))
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(original, source.text)
        assertEquals(0, source.commitCount)
        session.value = before
        session.save()
        assertEquals(0, source.commitCount)
    }

    @Test fun serializerOmittedIdentityIsNotReplacedByAnotherField() {
        val source = MemoryYamlSource("- value: 1\n  extra: unknown-owner\n")
        val config = yamlConfig<List<Hidden>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(Hidden(value = 2))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(null, identityItems(source).single().text("extra"))
    }

    @Test fun numericIdentityKeepsItsOriginalScalarSpellingWhenMovedAndUpdated() {
        val source = MemoryYamlSource("- id: 0x01\n  port: 1\n  extra: A\n- id: +2\n  port: 2\n  extra: B\n")
        val config = yamlConfig<List<Numeric>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = listOf(Numeric(2, 20), Numeric(1, 10))
        session.save()
        assertEquals(session.value, config.load().value)
        assertEquals(listOf("B", "A"), identityItems(source).map { it.text("extra") })
        assertTrue(source.text.contains("id: +2"), source.text)
        assertTrue(source.text.contains("id: 0x01"), source.text)
    }
}
