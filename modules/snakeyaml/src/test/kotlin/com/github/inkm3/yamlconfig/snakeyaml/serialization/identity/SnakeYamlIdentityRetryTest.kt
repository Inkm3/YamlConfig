package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.yamlConfig
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SnakeYamlIdentityRetryTest {
    private class Source(initial: String) : YamlSource {
        override val description = "identity-retry"
        var text = initial
        var fail = true
        var attempts = 0
        var commits = 0
        override fun openReader(): Reader = StringReader(text)
        override fun beginWrite(): YamlWriteTransaction {
            attempts++
            val buffer = StringWriter()
            return object : YamlWriteTransaction {
                override val writer = buffer
                override fun commit() {
                    if (fail) throw IOException("commit failed before publishing")
                    text = buffer.toString()
                    commits++
                }
                override fun close() = Unit
            }
        }
    }
    private val initial = "- name: a\n  port: 1 # A-port\n  extra: A\n- name: b\n  port: 2 # B-port\n  extra: B\n"

    @Test fun failedWriteDoesNotAdvanceIdentityPositionsOrMutatePresentation() {
        for (mode in YamlSaveMode.entries) {
            val source = Source(initial)
            val config = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source, saveMode = mode)
            val session = config.load()
            session.value = listOf(IdentityServer("b", 20), IdentityServer("a", 10))
            assertFailsWith<YamlWriteException> { session.save() }
            assertEquals(initial, source.text)
            assertEquals(0, source.commits)
            source.fail = false
            session.save()
            assertEquals(session.value, config.load().value)
            assertEquals(listOf("B", "A"), identityItems(source).map { it.text("extra") })
            assertTrue(source.text.contains("port: 20 # B-port"), source.text)
            assertTrue(source.text.contains("port: 10 # A-port"), source.text)
            session.save()
            assertEquals(2, source.attempts)
            assertEquals(1, source.commits)
        }
    }

    @Test fun revertingValueAfterFailedMoveUpdateIsANoOp() {
        val source = Source(initial)
        val session = yamlConfig<List<IdentityServer>>(SnakeYamlEngine(), source).load()
        val before = session.value
        session.value = listOf(IdentityServer("b", 20), IdentityServer("a", 10))
        assertFailsWith<YamlWriteException> { session.save() }
        session.value = before
        session.save()
        assertEquals(initial, source.text)
        assertEquals(1, source.attempts)
    }
}
