package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnakeYamlStructuralScalarListTest {
    private val engine = SnakeYamlEngine()

    @Test fun movePreservesQuotesAndCommentsOfEachElement() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- 'a' # a-comment\n- b # b-comment\n")
            val config = yamlConfig<List<String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf("b", "a")
            session.save()
            assertEquals(listOf("b", "a"), config.load().value)
            assertTrue(source.text.contains("'a' # a-comment"), source.text)
            assertTrue(source.text.contains("b # b-comment"), source.text)
            assertTrue(source.text.indexOf("# b-comment") < source.text.indexOf("# a-comment"), source.text)
        }
    }

    @Test fun insertPreservesBothExistingNeighborComments() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- 'a' # a-comment\n- b # b-comment\n")
            val config = yamlConfig<List<String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf("a", "new", "b")
            session.save()
            assertEquals(listOf("a", "new", "b"), config.load().value)
            assertTrue(source.text.contains("'a' # a-comment"), source.text)
            assertTrue(source.text.contains("b # b-comment"), source.text)
        }
    }

    @Test fun scalarUpdateRetainsTheUpdatedSlotsQuoteStyleAndComment() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- 'old' # slot-comment\n- keep # keep-comment\n")
            val config = yamlConfig<List<String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf("new", "keep")
            session.save()
            assertEquals(listOf("new", "keep"), config.load().value)
            assertTrue(source.text.contains("'new' # slot-comment"), source.text)
            assertTrue(source.text.contains("keep # keep-comment"), source.text)
        }
    }

    @Test fun removalLeavesSurvivingElementCommentAndStyle() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- drop # drop-comment\n- 'keep' # keep-comment\n")
            val config = yamlConfig<List<String>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf("keep")
            session.save()
            assertEquals(listOf("keep"), config.load().value)
            assertTrue(source.text.contains("'keep' # keep-comment"), source.text)
            assertFalse(source.text.contains("# drop-comment"), source.text)
        }
    }

    @Test fun untouchedNumericLexemesSurviveInsertionAndMove() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- +1 # one\n- 0x02 # two\n")
            val config = yamlConfig<List<Int>>(engine, source, saveMode = mode)
            val session = config.load()
            session.value = listOf(2, 3, 1)
            session.save()
            assertEquals(listOf(2, 3, 1), config.load().value)
            assertTrue(source.text.contains("0x02 # two"), source.text)
            assertTrue(source.text.contains("+1 # one"), source.text)
        }
    }

    @Test fun absentInheritedSequenceCreatesANewWholeOverride() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("")
            val defaults = StringYamlInput("- a\n- b\n")
            val config = yamlConfig<List<String>>(engine, source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = listOf("a", "new", "b")
            session.save()
            assertEquals(listOf("a", "new", "b"), config.load().value)
            assertEquals(1, source.commitCount)
        }
    }

    @Test fun secondSaveDoesNotRewriteStructuralResults() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("- a # a-comment\n- b # b-comment\n")
            val session = yamlConfig<List<String>>(engine, source, saveMode = mode).load()
            session.value = listOf("b", "a", "c")
            session.save()
            val saved = source.text
            session.save()
            assertEquals(saved, source.text)
            assertEquals(1, source.commitCount)
        }
    }
}
