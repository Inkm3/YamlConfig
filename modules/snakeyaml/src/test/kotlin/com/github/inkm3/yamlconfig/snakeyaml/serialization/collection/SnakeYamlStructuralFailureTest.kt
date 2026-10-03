package com.github.inkm3.yamlconfig.snakeyaml.serialization.collection

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.spi.YamlDocument
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SnakeYamlStructuralFailureTest {
    @Test fun failedCommitRetriesStructuralEditsFromUnmodifiedBaseline() {
        for (mode in YamlSaveMode.entries) {
            val original = "- name: a\n  port: 25565 # a\n  plugin: keep-a\n- name: b\n  port: 25566 # b\n"
            val memory = MemoryYamlSource(original)
            var fail = true
            val source = object : YamlSource by memory {
                override fun beginWrite(): YamlWriteTransaction {
                    val transaction = memory.beginWrite()
                    return object : YamlWriteTransaction by transaction {
                        override fun commit() {
                            if (fail) throw IOException("failure before publication")
                            transaction.commit()
                        }
                    }
                }
            }
            val config = yamlConfig<List<CollectionServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(session.value[1], session.value[0].copy(port = 30000))
            assertFailsWith<YamlWriteException> { session.save() }
            assertEquals(original, memory.text)
            assertEquals(0, memory.commitCount)
            fail = false
            session.save()
            assertEquals(session.value, config.load().value)
            assertTrue(memory.text.contains("30000 # a"), memory.text)
            assertTrue(memory.text.contains("keep-a"), memory.text)
            assertEquals(1, memory.commitCount)
            session.save()
            assertEquals(1, memory.commitCount)
        }
    }

    @Test fun actualEditorValidationRejectsADroppedMoveAndCanRetry() {
        val real = SnakeYamlEngine()
        var dropMove = true
        class ControlledEditor(val delegate: YamlEditor) : YamlEditor by delegate {
            override fun fork(): YamlEditor = ControlledEditor(delegate.fork())
            override fun sequence(path: YamlPath): YamlSequenceEditor {
                val sequence = delegate.sequence(path)
                return object : YamlSequenceEditor by sequence {
                    override fun move(fromIndex: Int, toIndex: Int) {
                        if (!dropMove) sequence.move(fromIndex, toIndex)
                    }
                }
            }
        }
        val engine = object : YamlEngine by real {
            override fun parse(input: YamlInput): YamlDocument {
                val document = real.parse(input)
                return object : YamlDocument by document {
                    override fun editor(): YamlEditor = ControlledEditor(document.editor())
                }
            }
            override fun write(editor: YamlEditor, output: YamlOutput) {
                real.write((editor as ControlledEditor).delegate, output)
            }
        }
        val original = "- a # a\n- b # b\n"
        val source = MemoryYamlSource(original)
        val config = yamlConfig<List<String>>(engine, source)
        val session = config.load()
        session.value = listOf("b", "a")
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(original, source.text)
        assertEquals(0, source.commitCount)
        dropMove = false
        session.save()
        assertEquals(listOf("b", "a"), config.load().value)
        assertTrue(source.text.indexOf("# b") < source.text.indexOf("# a"), source.text)
        assertEquals(1, source.commitCount)
        session.save()
        assertEquals(1, source.commitCount)
    }
}
