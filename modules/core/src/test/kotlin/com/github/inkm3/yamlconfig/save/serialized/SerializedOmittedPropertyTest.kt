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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SerializedOmittedPropertyTest {
    @Serializable private data class Config(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10,
        val derived: Int = base + 1,
    )
    @Serializable private data class TwoOmitted(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val derived: Int = base + 1,
    )
    @Serializable private data class Renamed(
        @SerialName("server-port") @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10, val derived: Int = base + 1,
    )
    @Serializable private data class Always(@EncodeDefault(EncodeDefault.Mode.ALWAYS) val port: Int = 25565)

    @Test fun forcedDependencyRepairKeepsAnOmittedExistingOverride() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("port" to i(25565), "base" to i(100), "future" to s("keep")))
            val config = yamlConfig<Config>(f.engine, f.source,
                defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Config(25565, 10, 101), config.load().value)
            assertEquals(1, f.writes)
            session.save()
            assertEquals(1, f.writes)
        }
    }

    @Test fun dependentOmissionIsRemovedWithoutRemovingAnotherValidOmission() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("port" to i(25565), "base" to i(100), "derived" to i(101)))
            val config = yamlConfig<TwoOmitted>(f.engine, f.source,
                defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))), saveMode = mode)
            val session = config.load()
            session.value = TwoOmitted()
            session.save()
            assertEquals(TwoOmitted(), config.load().value)
            assertEquals(i(25565), (f.source.rootNode as com.github.inkm3.yamlconfig.node.YamlMappingNode)["port"])
        }
    }

    @Test fun serializedNamesAreUsedWhenRetainingAnOverride() {
        val f = SaveFixture(stringMappingOf("server-port" to i(25565), "base" to i(100)))
        val config = yamlConfig<Renamed>(f.engine, f.source,
            defaultsSource = TestYamlSource(stringMappingOf("server-port" to i(30000))))
        val session = config.load()
        session.value = session.value.copy(base = 10)
        session.save()
        assertEquals(Renamed(25565, 10, 101), config.load().value)
    }

    @Test fun settingAnOmittedPropertyToItsDefaultStillWorksWithoutConflict() {
        val f = SaveFixture(stringMappingOf("port" to i(40000)))
        val config = yamlConfig<Config>(f.engine, f.source)
        val session = config.load()
        session.value = session.value.copy(port = 25565)
        session.save()
        assertEquals(Config(), config.load().value)
    }

    @Test fun unrepresentableDefaultIsNotInventedAndFailureCanBeRetried() {
        for (mode in YamlSaveMode.entries) {
            val original = stringMappingOf("port" to i(40000))
            val f = SaveFixture(original)
            val config = yamlConfig<Config>(f.engine, f.source,
                defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))), saveMode = mode)
            val session = config.load()
            session.value = Config()
            val error = assertFailsWith<SerializationException> { session.save() }
            assertTrue(error.message.orEmpty().contains("EncodeDefault(ALWAYS)"))
            assertEquals(0, f.writes)
            assertEquals(original, f.source.rootNode)
            session.value = Config(port = 50000)
            session.save()
            assertEquals(Config(port = 50000), config.load().value)
        }
    }

    @Test fun standardAlwaysAnnotationSolvesExplicitDefaultOverrideWithoutCustomAnnotations() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("port" to i(40000)))
            val config = yamlConfig<Always>(f.engine, f.source,
                defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))),
                serialization = YamlSerialization(encodeDefaults = false), saveMode = mode)
            val session = config.load()
            session.value = Always()
            session.save()
            assertEquals(Always(), config.load().value)
        }
    }

    @Test fun publicEncodingStillHonorsNeverAndAlways() {
        val format = YamlSerialization(encodeDefaults = true)
        val node = format.encodeToNode(Config()) as com.github.inkm3.yamlconfig.node.YamlMappingNode
        assertEquals(null, node["port"])
        assertEquals(stringMappingOf("port" to i(25565)),
            YamlSerialization(encodeDefaults = false).encodeToNode(Always()))
    }

    @Test fun unchangedSaveDoesNotRewriteExplicitOmittedValues() {
        val original = stringMappingOf("port" to i(25565))
        val f = SaveFixture(original)
        yamlConfig<Config>(f.engine, f.source).load().save()
        assertEquals(0, f.writes)
        assertEquals(original, f.source.rootNode)
    }
}
