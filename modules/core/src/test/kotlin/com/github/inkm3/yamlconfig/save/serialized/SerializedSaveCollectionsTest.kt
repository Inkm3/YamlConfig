package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SerializedSaveCollectionsTest {
    @Test fun inheritedListChangeCreatesAWholeOverride() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(null)
            val defaults = TestYamlSource(sequenceOf(s("a"), s("b")))
            val config = yamlConfig<List<String>>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = listOf("b", "c")
            session.save()
            assertEquals(listOf("b", "c"), config.load().value)
        }
    }

    @Test fun emptyListAndMapStayExplicitAgainstNonEmptyDefaults() {
        for (mode in YamlSaveMode.entries) {
            val defaults = TestYamlSource(stringMappingOf("servers" to sequenceOf(s("default")),
                "limits" to stringMappingOf("a" to i(1))))
            val f = SaveFixture(stringMappingOf())
            val config = yamlConfig<SavedConfig>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(servers = emptyList(), limits = emptyMap())
            session.save()
            assertEquals(emptyList(), config.load().value.servers)
            assertEquals(emptyMap(), config.load().value.limits)
        }
    }

    @Test fun explicitNullOverridesBothDefaults() {
        for (mode in YamlSaveMode.entries) {
            val defaults = TestYamlSource(stringMappingOf("message" to s("yaml")))
            val f = SaveFixture(stringMappingOf("message" to s("user")))
            val config = yamlConfig<SavedConfig>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(message = null)
            session.save()
            assertNull(config.load().value.message)
        }
    }

    @Test fun minimalModeRemovesWholeCollectionOverrideMatchingDefault() {
        val defaults = TestYamlSource(sequenceOf(s("default")))
        val f = SaveFixture(sequenceOf(s("user")))
        val config = yamlConfig<List<String>>(f.engine, f.source, defaultsSource = defaults,
            saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = listOf("default")
        session.save()
        assertNull(f.source.rootNode)
        assertEquals(listOf("default"), config.load().value)
    }

    @Test fun minimalModeAlsoPrunesUnchangedRedundantOverrides() {
        val f = SaveFixture(stringMappingOf("port" to i(25565), "future" to s("keep")))
        val config = yamlConfig<SavedConfig>(f.engine, f.source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.save()
        assertEquals(stringMappingOf("future" to s("keep")), f.source.rootNode)
        session.save()
        assertEquals(1, f.writes)
    }

    @Test fun minimalDoesNotDeleteUnknownDataInsideKnownObject() {
        val f = SaveFixture(stringMappingOf("database" to stringMappingOf(
            "host" to s("prod"), "port" to i(3306), "future" to s("keep"))))
        val config = yamlConfig<SavedConfig>(f.engine, f.source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val initial = config.load().value
        config.load().save()
        assertEquals(initial, config.load().value)
        assertEquals(stringMappingOf("database" to stringMappingOf("host" to s("prod"), "future" to s("keep"))),
            f.source.rootNode)
    }
}
