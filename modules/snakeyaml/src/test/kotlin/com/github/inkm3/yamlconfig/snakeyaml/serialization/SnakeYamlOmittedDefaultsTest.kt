package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SnakeYamlOmittedDefaultsTest {
    @Serializable private data class Config(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10, val derived: Int = base + 1,
    )
    @Serializable private data class Chain(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val derived: Int = base + 1,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val last: Int = derived + 1,
    )
    @Serializable private data class NullableConfig(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val message: String? = null,
        val base: Int = 10, val derived: Int = base + 1,
    )
    @Serializable private data class ListConfig(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val servers: List<String> = listOf("a"),
        val base: Int = 10, val derived: Int = base + 1,
    )
    @Serializable private data class Child(val port: Int = 1)
    @Serializable private data class Parent(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val child: Child = Child(),
        val base: Int = 10, val derived: Int = base + 1,
    )

    @Test fun forceRepairKeepsExplicitOmittedOverrideAndItsLexeme() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("port: +25565 # keep-port\nbase: 100\nfuture: 'keep' # plugin\n")
            val config = yamlConfig<Config>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("port: 30000\n"), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Config(25565, 10, 101), config.load().value)
            assertTrue(source.text.contains("port: +25565 # keep-port"), source.text)
            assertTrue(source.text.contains("future: 'keep' # plugin"), source.text)
            session.save()
            assertEquals(1, source.commitCount)
        }
    }

    @Test fun cascadingDependentOmissionsAreRecheckedUntilTheDocumentMatches() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("port: +25565 # keep-port\nbase: 100\nderived: 101\nlast: 102\n")
            val config = yamlConfig<Chain>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("port: 30000\n"), saveMode = mode)
            val session = config.load()
            session.value = Chain()
            session.save()
            assertEquals(Chain(), config.load().value)
            assertTrue(source.text.contains("port: +25565 # keep-port"), source.text)
            assertFalse(source.text.contains("derived:"), source.text)
            assertFalse(source.text.contains("last:"), source.text)
        }
    }

    @Test fun explicitOmittedNullRemainsAnOverrideNotMissing() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("message: ~ # explicit-null\nbase: 100\n")
            val config = yamlConfig<NullableConfig>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("message: yaml-default\n"), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(NullableConfig(null, 10, 101), config.load().value)
            assertTrue(source.text.contains("message: ~ # explicit-null"), source.text)
        }
    }

    @Test fun wholeOmittedCollectionCanBeRetainedAgainstDifferentYamlDefaults() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("servers: ['a'] # own-list\nbase: 100\n")
            val config = yamlConfig<ListConfig>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("servers: [b]\n"), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(ListConfig(listOf("a"), 10, 101), config.load().value)
            assertTrue(source.text.contains("'a'"), source.text)
            assertTrue(source.text.contains("# own-list"), source.text)
        }
    }

    @Test fun omittedObjectKeepsUnknownChildrenWhenRetained() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("child:\n  port: +1 # child-port\n  extra: 'keep' # child-extra\nbase: 100\n")
            val config = yamlConfig<Parent>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("child:\n  port: 99\n"), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Parent(Child(), 10, 101), config.load().value)
            assertTrue(source.text.contains("port: +1 # child-port"), source.text)
            assertTrue(source.text.contains("extra: 'keep' # child-extra"), source.text)
        }
    }

    @Test fun unchangedPreserveSaveLeavesOriginalBytesUntouched() {
        val original = "# heading\nport: +25565\n\nbase: 10\n"
        val source = MemoryYamlSource(original)
        yamlConfig<Config>(SnakeYamlEngine(), source).load().save()
        assertEquals(original, source.text)
        assertEquals(0, source.commitCount)
    }
}
