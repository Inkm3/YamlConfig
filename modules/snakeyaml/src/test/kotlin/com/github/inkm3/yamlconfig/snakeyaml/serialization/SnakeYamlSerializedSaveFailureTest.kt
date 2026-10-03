package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.yamlConfig
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SnakeYamlSerializedSaveFailureTest {
    private enum class Stage { BEGIN, WRITE, COMMIT }
    private class FailingSource(initial: String, private val stage: Stage) : YamlSource {
        override val description = "staged-failing-source"
        var text = initial
            private set
        var fail = true
        var attempts = 0
        var commits = 0
        var closes = 0
        override fun openReader(): Reader = StringReader(text)
        override fun beginWrite(): YamlWriteTransaction {
            attempts++
            if (fail && stage == Stage.BEGIN) throw IOException("begin failed")
            val buffer = StringWriter()
            return object : YamlWriteTransaction {
                override val writer: Writer = object : Writer() {
                    override fun write(cbuf: CharArray, off: Int, len: Int) {
                        if (fail && stage == Stage.WRITE) {
                            buffer.write(cbuf, off, len / 2)
                            throw IOException("staged partial write failed")
                        }
                        buffer.write(cbuf, off, len)
                    }
                    override fun flush() = Unit
                    override fun close() = Unit
                }
                override fun commit() {
                    if (fail && stage == Stage.COMMIT) throw IOException("commit failed before publishing")
                    text = buffer.toString()
                    commits++
                }
                override fun close() { closes++ }
            }
        }
    }

    private fun failsThenRetries(stage: Stage) {
        val original = "port: 25565 # original\n"
        val source = FailingSource(original, stage)
        val config = yamlConfig<SnakeSavedConfig>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = session.value.copy(port = 30000)
        assertFailsWith<YamlWriteException> { session.save() }
        assertEquals(original, source.text)
        assertEquals(0, source.commits)
        assertEquals(if (stage == Stage.BEGIN) 0 else 1, source.closes)
        source.fail = false
        session.save()
        assertEquals(30000, config.load().value.port)
        assertEquals(1, source.commits)
        session.save()
        assertEquals(2, source.attempts)
    }

    @Test fun beginWriteFailurePreservesSessionAndSource() { failsThenRetries(Stage.BEGIN) }
    @Test fun partialStagedWriteFailurePreservesSessionAndSource() { failsThenRetries(Stage.WRITE) }
    @Test fun commitFailureBeforePublicationPreservesSessionAndSource() { failsThenRetries(Stage.COMMIT) }

    @Test fun failedSecondWriteRetriesFromLastCommittedValue() {
        val source = FailingSource("port: 1\n", Stage.COMMIT)
        source.fail = false
        val config = yamlConfig<SnakeSavedConfig>(SnakeYamlEngine(), source)
        val session = config.load()
        session.value = session.value.copy(port = 2)
        session.save()
        val previousText = source.text
        source.fail = true
        session.value = session.value.copy(port = 3)
        assertFailsWith<YamlWriteException> { session.save() }
        assertEquals(previousText, source.text)
        assertEquals(2, config.load().value.port)
        source.fail = false
        session.save()
        assertEquals(3, config.load().value.port)
        assertEquals(2, source.commits)
    }

    @Test fun revertingValueAfterFailedWriteDoesNotWriteAgain() {
        val source = FailingSource("port: 1\n", Stage.COMMIT)
        val session = yamlConfig<SnakeSavedConfig>(SnakeYamlEngine(), source).load()
        val initial = session.value
        session.value = initial.copy(port = 2)
        assertFailsWith<YamlWriteException> { session.save() }
        session.value = initial
        session.save()
        assertEquals(1, source.attempts)
        assertEquals("port: 1\n", source.text)
    }
}
