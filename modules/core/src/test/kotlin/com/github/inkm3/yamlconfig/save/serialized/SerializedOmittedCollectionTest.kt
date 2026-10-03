package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class SerializedOmittedCollectionTest {
    @Serializable private data class Entry(
        val id: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val derived: Int = base + 1,
    )
    @Serializable private data class Parent(val child: Entry, val base: Int = 10, val derived: Int = base + 1)
    private fun raw(id: String, base: Int = 10) = stringMappingOf("id" to s(id), "port" to i(25565),
        "base" to i(base), "derived" to i(base + 1), "future" to s("owner-$id"))

    @Test fun movedElementKeepsOmittedRawValuesAndItsUnknownData() {
        val f = SaveFixture(sequenceOf(raw("a"), raw("b")))
        val config = yamlConfig<List<Entry>>(f.engine, f.source)
        val session = config.load()
        session.value = listOf(Entry("b", base = 20), Entry("a"))
        session.save()
        assertEquals(session.value, config.load().value)
        val first = (f.source.rootNode as YamlSequenceNode)[0] as YamlMappingNode
        assertEquals(s("owner-b"), first["future"])
        assertEquals(i(25565), first["port"])
        assertEquals(null, first["derived"])
    }

    @Test fun removedListElementIsNotRestoredByOmissionHandling() {
        val f = SaveFixture(sequenceOf(raw("a"), raw("b")))
        val config = yamlConfig<List<Entry>>(f.engine, f.source)
        val session = config.load()
        session.value = listOf(Entry("b"))
        session.save()
        assertEquals(listOf(Entry("b")), config.load().value)
        val values = f.source.rootNode as YamlSequenceNode
        assertEquals(1, values.size)
        assertEquals(s("owner-b"), (values[0] as YamlMappingNode)["future"])
    }

    @Test fun mapEntryRemovalAndOmittedValueRetentionAreDifferentOperations() {
        val f = SaveFixture(stringMappingOf("a" to raw("a"), "b" to raw("b")))
        val config = yamlConfig<Map<String, Entry>>(f.engine, f.source)
        val session = config.load()
        session.value = mapOf("b" to Entry("b", base = 20))
        session.save()
        assertEquals(session.value, config.load().value)
        val root = f.source.rootNode as YamlMappingNode
        assertEquals(null, root["a"])
        val b = root["b"] as YamlMappingNode
        assertEquals(s("owner-b"), b["future"])
        assertEquals(i(25565), b["port"])
        assertEquals(null, b["derived"])
    }

    @Test fun nestedParentKeepsAnOmittedOverrideAgainstYamlDefaultsDuringForceRepair() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("child" to raw("a"), "base" to i(100)))
            val defaults = TestYamlSource(stringMappingOf("child" to stringMappingOf("port" to i(30000))))
            val config = yamlConfig<Parent>(f.engine, f.source, defaultsSource = defaults, saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Parent(Entry("a"), 10, 101), config.load().value)
        }
    }

    @Test fun newElementHasNoInventedRawOmissionOrUnknownFields() {
        val f = SaveFixture(sequenceOf(raw("a")))
        val config = yamlConfig<List<Entry>>(f.engine, f.source)
        val session = config.load()
        session.value = listOf(Entry("new"))
        session.save()
        assertEquals(listOf(Entry("new")), config.load().value)
        val item = (f.source.rootNode as YamlSequenceNode)[0] as YamlMappingNode
        assertEquals(null, item["future"])
        assertEquals(null, item["port"])
    }
}
