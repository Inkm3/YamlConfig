package com.github.inkm3.yamlconfig.source.internal

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertFailsWith

class YamlMissingFileHandlerTest {

    @Test
    fun createEmptyWrapsIoFailureAsYamlWriteException() {
        val source = FailingYamlSource()

        assertFailsWith<YamlWriteException> {
            YamlMissingFileHandler.handle(
                userSource = source,
                defaultsSource = null,
                policy = YamlMissingFilePolicy.CREATE_EMPTY,
            )
        }
    }

    private class FailingYamlSource : YamlSource {

        override val description: String = "failing-source"

        override fun exists(): Boolean = false

        override fun openReader(): Reader =
            StringReader("")

        override fun beginWrite(): YamlWriteTransaction {
            throw IOException("simulated failure")
        }
    }
}