package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SnakeYamlSerializedSaveIntegrationTest {
    private val engine = SnakeYamlEngine()
    @Serializable
    private data class Dependent(val base: Int = 10, val derived: Int = base + 1)
    @Serializable
    private data class Omitted(@EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565)

    @Test fun knownScalarUpdateKeepsCommentsUnknownDataAndUnchangedLexemes() {
        val source = MemoryYamlSource("""
            # config header
            server-name: 'lobby'
            port: +25565 # port comment
            # future comment
            future-number: +25567
        """.trimIndent())
        val config = yamlConfig<SnakeSavedConfig>(engine, source)
        val session = config.load()
        session.value = session.value.copy(port = 30000)
        session.save()
        assertEquals(30000, config.load().value.port)
        assertTrue(source.text.contains("# config header"), source.text)
        assertTrue(source.text.contains("server-name: 'lobby'"), source.text)
        assertTrue(Regex("""(?m)^port:\s*30000\s+# port comment\s*$""").containsMatchIn(source.text), source.text)
        assertTrue(source.text.contains("# future comment"), source.text)
        assertTrue(source.text.contains("future-number: +25567"), source.text)
    }

    @Test fun nestedKnownUpdateKeepsUnknownSiblingAndCommentInBothModes() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("""
                database:
                  host: 'db'
                  port: +3306 # db port comment
                  # plugin configuration
                  plugin-option: keep-me
            """.trimIndent())
            val config = yamlConfig<SnakeSavedConfig>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(database = session.value.database.copy(port = 5432))
            session.save()
            assertEquals(SnakeSavedDatabase("db", 5432), config.load().value.database)
            assertTrue(source.text.contains("host: 'db'"), source.text)
            assertTrue(source.text.contains("# db port comment"), source.text)
            assertTrue(source.text.contains("# plugin configuration"), source.text)
            assertTrue(source.text.contains("plugin-option: keep-me"), source.text)
        }
    }

    @Test fun noOpSaveDoesNotEvenRewriteFormatting() {
        val text = "# header\n\nserver-name: 'lobby'\nport: +25565 # keep\n"
        val source = MemoryYamlSource(text)
        val session = yamlConfig<SnakeSavedConfig>(engine, source).load()
        session.save()
        assertEquals(text, source.text)
        assertEquals(0, source.commitCount)
        session.value = session.value.copy(port = 30000)
        session.save()
        val saved = source.text
        session.save()
        assertEquals(saved, source.text)
        assertEquals(1, source.commitCount)
    }

    @Test fun missingNestedObjectRetainsParentsConstructorDefaultAfterSave() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("")
            val config = yamlConfig<SnakeSavedConfig>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(database = session.value.database.copy(port = 5432))
            session.save()
            assertEquals(SnakeSavedDatabase("prod", 5432), config.load().value.database)
        }
    }

    @Test fun changedBaseDoesNotResetUnchangedInheritedDependentValue() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("base: 100\n")
            val config = yamlConfig<Dependent>(engine, source, saveMode = mode)
            val session = config.load()
            assertEquals(Dependent(100, 101), session.value)
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Dependent(10, 101), config.load().value)
        }
    }

    @Test fun kotlinDefaultStillOverridesDifferentYamlDefaultWhenEncodingDefaultsDisabled() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("port: 40000\n")
            val defaults = StringYamlInput("port: 30000\n")
            val config = yamlConfig<SnakeSavedConfig>(engine, source, defaultsSource = defaults,
                saveMode = mode, serialization = YamlSerialization(encodeDefaults = false))
            val session = config.load()
            session.value = session.value.copy(port = 25565)
            session.save()
            assertEquals(25565, config.load().value.port)
        }
    }

    @Test fun unrepresentableAnnotatedOverrideFailsWithoutAlteringTextAndCanRetry() {
        for (mode in YamlSaveMode.entries) {
            val text = "port: 40000 # do not lose this\n"
            val source = MemoryYamlSource(text)
            val config = yamlConfig<Omitted>(engine, source, defaultsSource = StringYamlInput("port: 30000"),
                saveMode = mode)
            val session = config.load()
            session.value = Omitted()
            assertFailsWith<SerializationException> { session.save() }
            assertEquals(text, source.text)
            assertEquals(0, source.commitCount)
            session.value = Omitted(50000)
            session.save()
            assertEquals(Omitted(50000), config.load().value)
            assertTrue(source.text.contains("# do not lose this"), source.text)
        }
    }

    @Test fun inheritedRootListCanCreateAWholeOverrideInAnEmptyDocument() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("")
            val config = yamlConfig<List<String>>(engine, source,
                defaultsSource = StringYamlInput("- lobby\n- survival\n"), saveMode = mode)
            val session = config.load()
            session.value = listOf("creative", "lobby")
            session.save()
            assertEquals(listOf("creative", "lobby"), config.load().value)
        }
    }

    @Test fun minimalCollectionOverrideMatchingDefaultsIsRemoved() {
        val source = MemoryYamlSource("- custom\n")
        val config = yamlConfig<List<String>>(engine, source, defaultsSource = StringYamlInput("- default\n"),
            saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = listOf("default")
        session.save()
        assertNull(engine.parse(source).root)
        assertEquals(listOf("default"), config.load().value)
    }

    @Test fun explicitNullAndEmptyCollectionsSurviveRealYamlRoundTrip() {
        for (mode in YamlSaveMode.entries) {
            val defaults = StringYamlInput("message: default\nservers: [lobby]\nlimits: {a: 1}\n")
            val source = MemoryYamlSource("{}\n")
            val config = yamlConfig<SnakeSavedConfig>(engine, source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(message = null, servers = emptyList(), limits = emptyMap())
            val expected = session.value
            session.save()
            assertEquals(expected, config.load().value)
        }
    }

    @Test fun minimalPruningKeepsUnknownOptionAndItsComment() {
        val source = MemoryYamlSource("port: 25565\n\n# future setting\nfuture-option: keep\n")
        val config = yamlConfig<SnakeSavedConfig>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val initial = config.load().value
        config.load().save()
        assertEquals(initial, config.load().value)
        assertTrue(source.text.contains("# future setting"), source.text)
        assertTrue(source.text.contains("future-option: keep"), source.text)
        assertTrue(!source.text.contains("port:"), source.text)
    }
}
