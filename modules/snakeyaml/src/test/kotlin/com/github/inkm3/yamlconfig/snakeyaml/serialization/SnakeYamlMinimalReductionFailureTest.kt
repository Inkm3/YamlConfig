package com.github.inkm3.yamlconfig.snakeyaml.serialization

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
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.test.*

class SnakeYamlMinimalReductionFailureTest {
    @Serializable private data class Config(val token: String, val base: Int = 10, val derived: Int = base + 1)
    private val original = "token: t\nbase: 100\nderived: +101 # keep\nfuture: value # future\n"

    @Test fun failedCommitDiscardsReducedEditorAndRetriesFromTheOriginalBaseline() {
        val memory = MemoryYamlSource(original)
        var fail = true
        val source = object : YamlSource by memory {
            override fun beginWrite(): YamlWriteTransaction {
                val write = memory.beginWrite()
                return object : YamlWriteTransaction by write {
                    override fun commit() {
                        if (fail) throw IOException("fail before publication")
                        write.commit()
                    }
                }
            }
        }
        val config = yamlConfig<Config>(SnakeYamlEngine(), source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = session.value.copy(base = 10)
        assertFailsWith<YamlWriteException> { session.save() }
        assertEquals(original, memory.text)
        assertEquals(0, memory.commitCount)
        fail = false
        session.save()
        assertEquals(Config("t", 10, 101), config.load().value)
        assertTrue(memory.text.contains("+101 # keep"), memory.text)
        assertTrue(memory.text.contains("value # future"), memory.text)
        assertEquals(1, memory.commitCount)
        session.save()
        assertEquals(1, memory.commitCount)
    }

    @Test fun actualEditorValidationRejectsAWrongRemovalEvenAfterCandidateSuccess() {
        val real = SnakeYamlEngine()
        var corrupt = true
        class ControlledEditor(val delegate: YamlEditor) : YamlEditor by delegate {
            override fun fork(): YamlEditor = ControlledEditor(delegate.fork())
            override fun remove(path: YamlPath) {
                delegate.remove(if (corrupt) YamlPath.root().child("token") else path)
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
        val source = MemoryYamlSource(original)
        val config = yamlConfig<Config>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE)
        val session = config.load()
        session.value = session.value.copy(base = 10)
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(original, source.text)
        assertEquals(0, source.commitCount)
        corrupt = false
        session.save()
        assertEquals(Config("t", 10, 101), config.load().value)
        assertEquals(1, source.commitCount)
    }
}
