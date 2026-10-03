package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class SerializedSaveDefaultsTest {
    @Serializable
    private data class Dependent(val base: Int = 10, val derived: Int = base + 1)
    @Serializable
    private data class Omitted(@EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565)

    @Test fun dependentDefaultsAreValidatedAgainstAllCurrentChanges() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("base" to i(100), "derived" to i(101)))
            val config = yamlConfig<Dependent>(f.engine, f.source, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Dependent(10, 101), config.load().value, mode.name)
        }
    }

    @Test fun unchangedInheritedDependentSiblingIsMaterializedWhenNeeded() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("base" to i(100)))
            val config = yamlConfig<Dependent>(f.engine, f.source, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Dependent(10, 101), config.load().value, mode.name)
        }
    }

    @Test fun missingParentMaterializationRetainsParentConstructorDefault() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf())
            val config = yamlConfig<SavedConfig>(f.engine, f.source, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(database = session.value.database.copy(port = 5432))
            session.save()
            assertEquals(SavedDatabase("prod", 5432), config.load().value.database, mode.name)
        }
    }

    @Test fun yamlParentDefaultsAndExplicitChildChangesRemainConsistent() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("database" to stringMappingOf("port" to i(5432))))
            val defaults = TestYamlSource(stringMappingOf("database" to stringMappingOf("host" to s("yaml"))))
            val config = yamlConfig<SavedConfig>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(database = session.value.database.copy(port = 6000))
            session.save()
            assertEquals(SavedDatabase("yaml", 6000), config.load().value.database, mode.name)
        }
    }

    @Test fun kotlinDefaultRemainsExplicitWhenDifferentFromYamlDefault() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("port" to i(40000)))
            val defaults = TestYamlSource(stringMappingOf("port" to i(30000)))
            val config = yamlConfig<SavedConfig>(f.engine, f.source, defaultsSource = defaults, saveMode = mode,
                serialization = YamlSerialization(encodeDefaults = false))
            val session = config.load()
            session.value = session.value.copy(port = 25565)
            session.save()
            assertEquals(25565, config.load().value.port, mode.name)
        }
    }

    @Test fun annotationOmissionThatCannotRepresentOverrideFailsBeforeWrite() {
        for (mode in YamlSaveMode.entries) {
            val original = stringMappingOf("port" to i(40000))
            val f = SaveFixture(original)
            val defaults = TestYamlSource(stringMappingOf("port" to i(30000)))
            val config = yamlConfig<Omitted>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = Omitted()
            assertFailsWith<SerializationException> { session.save() }
            assertEquals(original, f.source.rootNode)
            assertEquals(0, f.writes)
            session.value = Omitted(50000)
            session.save()
            assertEquals(Omitted(50000), config.load().value)
        }
    }

    @Test fun annotationOmissionWithoutConflictingDefaultCanBeSaved() {
        val f = SaveFixture(stringMappingOf("port" to i(40000)))
        val config = yamlConfig<Omitted>(f.engine, f.source)
        val session = config.load()
        session.value = Omitted()
        session.save()
        assertEquals(Omitted(), config.load().value)
    }

    @Test fun requiredValueCannotDisappearDuringMinimalReduction() {
        val f = SaveFixture(s("required"))
        val config = yamlConfig<String>(f.engine, f.source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = "changed"
        session.save()
        assertEquals("changed", config.load().value)
    }
}
