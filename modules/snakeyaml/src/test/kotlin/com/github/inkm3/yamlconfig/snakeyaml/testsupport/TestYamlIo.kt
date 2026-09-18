package com.github.inkm3.yamlconfig.snakeyaml.testsupport

import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import java.io.*

internal class StringYamlInput(
    private val text: String,
    override val description: String = "test-input",
) : YamlInput {
    override fun openReader(): Reader = StringReader(text)
}

internal class MemoryYamlSource(
    initialText: String = "",
    override val description: String = "memory-yaml",
) : YamlSource {
    internal var text: String = initialText
        private set

    internal var commitCount: Int = 0
        private set

    override fun openReader(): Reader = StringReader(text)

    override fun beginWrite(): YamlWriteTransaction {
        val buffer = StringWriter()

        return object : YamlWriteTransaction {
            override val writer: Writer = buffer

            override fun commit(): Unit {
                text = buffer.toString()
                commitCount++
            }

            override fun close(): Unit = Unit
        }
    }
}

internal class FailingYamlOutput(
    override val description: String = "failing-output",
) : YamlOutput {
    override fun beginWrite(): YamlWriteTransaction {
        return object : YamlWriteTransaction {
            override val writer: Writer = object : Writer() {
                override fun write(cbuf: CharArray, off: Int, len: Int): Unit {
                    throw IOException("simulated write failure")
                }

                override fun flush(): Unit = Unit
                override fun close(): Unit = Unit
            }

            override fun commit(): Unit = Unit
            override fun close(): Unit = Unit
        }
    }
}
