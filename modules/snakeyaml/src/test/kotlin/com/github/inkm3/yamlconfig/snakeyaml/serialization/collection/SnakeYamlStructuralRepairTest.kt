package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnakeYamlStructuralRepairTest {
    @Serializable
    private data class Config(val base: Int = 10, val derived: Int = base + 1, val values: List<Int> = emptyList())

    @Test fun dependencyRepairDoesNotRewriteUnrelatedCollectionLexemes() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("base: 100\nvalues:\n- +1 # keep-one\n- 0x02 # keep-two\n")
            val config = yamlConfig<Config>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            assertEquals(101, session.value.derived)
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Config(10, 101, listOf(1, 2)), config.load().value)
            assertTrue(source.text.contains("+1 # keep-one"), source.text)
            assertTrue(source.text.contains("0x02 # keep-two"), source.text)
        }
    }
}
