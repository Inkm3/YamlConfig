package com.github.inkm3.yamlconfig.benchmark

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.Serializable
import java.io.StringReader
import java.io.StringWriter

@Serializable
data class BenchmarkEntry(val name: String, val port: Int = 25565)

@Serializable
data class BenchmarkConfig(
    val token: String,
    val port: Int = 25565,
    val items: List<BenchmarkEntry> = emptyList(),
    val labels: Map<String, Int> = emptyMap(),
)

private class MemorySource(var text: String) : YamlSource {
    override val description: String = "benchmark-memory"
    override fun exists(): Boolean = true
    override fun openReader(): StringReader = StringReader(text)
    override fun beginWrite(): YamlWriteTransaction = object : YamlWriteTransaction {
        override val writer = StringWriter()
        override fun commit() { text = writer.toString() }
        override fun close() { writer.close() }
    }
}

/** Every save invocation starts with a fresh source/session, never a warmed no-op. */
class ConfigWorkload(size: Int) {
    private val engine = SnakeYamlEngine()
    private val format = YamlSerialization.Default
    private val value = BenchmarkConfig("benchmark", items = (0 until size).map { BenchmarkEntry("item-$it") },
        labels = (0 until size).associate { "key-$it" to it })
    private val text = buildString {
        append("token: benchmark\nport: +25565 # keep\nitems:\n")
        for (entry in value.items) append("- name: '${entry.name}'\n  port: 25565 # item\n")
        append("labels:\n")
        for ((key, number) in value.labels) append("  $key: $number\n")
    }
    private val reversed = value.copy(items = value.items.reversed().mapIndexed { index, item ->
        if (index == 0) item.copy(port = 30000) else item
    })

    init {
        require(size > 0)
        check(load() == value)
        val saved = moveAndUpdate()
        check(yamlConfig<BenchmarkConfig>(engine, MemorySource(saved)).load().value == reversed)
    }

    fun codecRoundTrip(): BenchmarkConfig = format.decodeFromNode(format.encodeToNode(value))
    fun load(): BenchmarkConfig = yamlConfig<BenchmarkConfig>(engine, MemorySource(text)).load().value
    fun moveAndUpdate(): String {
        val source = MemorySource(text)
        val session = yamlConfig<BenchmarkConfig>(engine, source).load()
        session.value = reversed
        session.save()
        return source.text
    }
    fun minimalReduction(): String {
        val source = MemorySource(text)
        val session = yamlConfig<BenchmarkConfig>(engine, source, saveMode = YamlSaveMode.MINIMAL_DIFFERENCE).load()
        session.save()
        return source.text
    }
}
