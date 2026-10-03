package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SnakeYamlOmittedCollectionTest {
    @Serializable private data class Entry(
        val id: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val derived: Int = base + 1,
    )

    @Test fun changedMovedElementKeepsOmittedPortAndUnknownDataWithItsOwner() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("""
                - id: a
                  port: +25565 # a-port
                  base: 100
                  derived: 101
                  extra: 'a-owned' # a-extra
                - id: b
                  port: +25565 # b-port
                  base: 100
                  derived: 101
                  extra: 'b-owned' # b-extra
            """.trimIndent() + "\n")
            val config = yamlConfig<List<Entry>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(Entry("b"), Entry("a", base = 100))
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.indexOf("id: b") < source.text.indexOf("id: a"), source.text)
            val first = source.text.substringBefore("- id: a")
            assertTrue(first.contains("port: +25565 # b-port"), source.text)
            assertTrue(first.contains("extra: 'b-owned' # b-extra"), source.text)
            assertFalse(first.contains("derived:"), source.text)
            assertTrue(source.text.contains("# a-port"), source.text)
        }
    }

    @Test fun numericMapKeyAndOmittedValueLexemesAreRetainedDuringRepair() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("0xff: # existing-key\n  id: a\n  port: +25565 # port\n  base: 100\n  derived: 101\n  extra: keep\n")
            val config = yamlConfig<Map<Int, Entry>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = mapOf(255 to Entry("a"))
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(source.text.contains("0xff:"), source.text)
            assertTrue(source.text.contains("# existing-key"), source.text)
            assertTrue(source.text.contains("port: +25565 # port"), source.text)
            assertTrue(source.text.contains("extra: keep"), source.text)
            assertFalse(source.text.contains("derived:"), source.text)
        }
    }

    @Test fun deletedMapEntryIsNotReintroducedAsAnOmittedValue() {
        val source = MemoryYamlSource("a:\n  id: a\n  extra: a-owned\nb:\n  id: b\n  port: +25565 # b-port\n  extra: b-owned\n")
        val config = yamlConfig<Map<String, Entry>>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = mapOf("b" to Entry("b", base = 20))
        session.save()
        assertEquals(session.value, config.load().value)
        assertFalse(source.text.contains("a-owned"), source.text)
        assertTrue(source.text.contains("b-owned"), source.text)
        assertTrue(source.text.contains("port: +25565 # b-port"), source.text)
    }
}
