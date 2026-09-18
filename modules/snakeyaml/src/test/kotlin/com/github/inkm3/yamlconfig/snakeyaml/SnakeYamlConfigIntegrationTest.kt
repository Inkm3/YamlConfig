package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.schema.list
import com.github.inkm3.yamlconfig.schema.map
import com.github.inkm3.yamlconfig.schema.str
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnakeYamlConfigIntegrationTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun mapStructuralSavePreservesExistingKeyLexeme() {
        val source = MemoryYamlSource(
            """
            0xff: old
            2: keep
            """.trimIndent()
        )
        val config = yamlConfig(
            engine = engine,
            userSource = source,
            schema = map(com.github.inkm3.yamlconfig.schema.int(), str()),
        )
        val session = config.load()
        session.value = linkedMapOf(255 to "new", 2 to "keep")
        session.save()

        assertTrue(source.text.contains("0xff:"), source.text)
        assertTrue(source.text.contains("new"), source.text)
    }

    @Test
    fun listMovePreservesExistingElementPresentation() {
        val source = MemoryYamlSource(
            """
            # comment-a
            - 'A'
            # comment-b
            - B
            # comment-c
            - C
            """.trimIndent()
        )
        val config = yamlConfig(
            engine = engine,
            userSource = source,
            schema = list(str()),
        )
        val session = config.load()
        session.value = listOf("C", "A", "B")
        session.save()

        assertTrue(source.text.indexOf("# comment-c") < source.text.indexOf("# comment-a"), source.text)
        assertTrue(source.text.contains("'A'"), source.text)
    }

    @Test
    fun listInsertPreservesUnchangedExistingElements() {
        val source = MemoryYamlSource("- 'A'\n- B")
        val config = yamlConfig(engine, source, list(str()))
        val session = config.load()
        session.value = listOf("X", "A", "B")
        session.save()

        assertTrue(source.text.contains("\"X\""), source.text)
        assertTrue(source.text.contains("'A'"), source.text)
    }

    @Test
    fun inheritedDefaultListIsWrittenAsWholeOverrideWhenChanged() {
        val source = MemoryYamlSource("")
        val defaults = StringYamlInput("- lobby\n- survival", "defaults")
        val config = yamlConfig(
            engine = engine,
            userSource = source,
            schema = list(str()),
            defaultsSource = defaults,
        )
        val session = config.load()
        assertEquals(listOf("lobby", "survival"), session.value)

        session.value = listOf("lobby", "creative")
        session.save()

        val reloaded = yamlConfig(engine, source, list(str())).load()
        assertEquals(listOf("lobby", "creative"), reloaded.value)
    }

    @Test
    fun minimalDifferenceRemovesWholeCollectionWhenEqualToDefault() {
        val source = MemoryYamlSource("- user")
        val defaults = StringYamlInput("- default", "defaults")
        val config = yamlConfig(
            engine = engine,
            userSource = source,
            schema = list(str()),
            defaultsSource = defaults,
            saveMode = YamlSaveMode.MINIMAL_DIFFERENCE,
        )
        val session = config.load()
        session.value = listOf("default")
        session.save()

        assertEquals("", source.text)
    }
}
