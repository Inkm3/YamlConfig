package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.internal.serialized.validation.YamlCandidateValidation
import com.github.inkm3.yamlconfig.save.serialized.SaveFixture
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SerializedValidationPerformanceTest {
    @Serializable private data class Config(val value: Int = 1)
    private class CountingSerializer<T>(private val delegate: KSerializer<T>) : KSerializer<T> {
        override val descriptor = delegate.descriptor
        var decodes = 0
        override fun deserialize(decoder: Decoder): T { decodes++; return delegate.deserialize(decoder) }
        override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)
    }

    @Test fun publicSaveStillValidatesTheCandidateAndTheActualEditor() {
        val f = SaveFixture(stringMappingOf("value" to i(1)))
        val serializer = CountingSerializer(serializer<Config>())
        val session = yamlConfig(f.engine, f.source, serializer).load()
        serializer.decodes = 0
        session.value = Config(2)
        session.save()
        assertEquals(2, serializer.decodes)
        assertEquals(stringMappingOf("value" to i(2)), f.source.rootNode)
        session.save() // PRESERVE no-op performs no decode or write.
        assertEquals(2, serializer.decodes)
        assertEquals(1, f.writes)
    }

    @Test fun failedWriteRetryRevalidatesRatherThanReusingTheFailedSaveMemo() {
        val f = SaveFixture(stringMappingOf("value" to i(1)))
        var fail = true
        val engine = object : YamlEngine by f.engine {
            override fun write(editor: YamlEditor, output: YamlOutput) {
                check(!fail) { "write failed" }
                f.engine.write(editor, output)
            }
        }
        val serializer = CountingSerializer(serializer<Config>())
        val session = yamlConfig(engine, f.source, serializer).load()
        serializer.decodes = 0
        session.value = Config(2)
        assertFailsWith<IllegalStateException> { session.save() }
        assertEquals(2, serializer.decodes)
        assertEquals(stringMappingOf("value" to i(1)), f.source.rootNode)
        fail = false
        session.save()
        assertEquals(4, serializer.decodes)
        assertEquals(1, f.writes)
        assertEquals(stringMappingOf("value" to i(2)), f.source.rootNode)
    }

    @Test fun sessionsSharingASerializerDoNotShareValidationResults() {
        val first = SaveFixture(stringMappingOf("value" to i(1)))
        val second = SaveFixture(stringMappingOf("value" to i(1)))
        val serializer = CountingSerializer(serializer<Config>())
        val a = yamlConfig(first.engine, first.source, serializer).load()
        val b = yamlConfig(second.engine, second.source, serializer).load()
        serializer.decodes = 0
        a.value = Config(2); b.value = Config(2)
        a.save(); b.save()
        assertEquals(4, serializer.decodes)
        assertEquals(1, first.writes)
        assertEquals(1, second.writes)
    }

    @Test fun realCustomDeserializerObservingMapOrderGetsDifferentResults() {
        val mapSerializer = serializer<Map<String, Int>>()
        val firstValue = object : KSerializer<Int> {
            override val descriptor = mapSerializer.descriptor
            override fun deserialize(decoder: Decoder): Int = mapSerializer.deserialize(decoder).values.first()
            override fun serialize(encoder: Encoder, value: Int) =
                mapSerializer.serialize(encoder, mapOf("value" to value))
        }
        var decodes = 0
        val validation = YamlCandidateValidation({ root ->
            decodes++
            i(YamlSerialization.Default.decodeFromNode(firstValue, requireNotNull(root)))
        })
        val a = stringMappingOf("a" to i(1), "b" to i(2))
        val b = stringMappingOf("b" to i(2), "a" to i(1))
        assertEquals(a, b)
        assertEquals(i(1), validation.evaluate(a))
        assertEquals(i(2), validation.evaluate(b))
        assertEquals(i(1), validation.evaluate(stringMappingOf("a" to i(1), "b" to i(2))))
        assertEquals(2, decodes)
    }

    @Test fun failedResultsAlsoObeyTheCapacityBound() {
        var calls = 0
        val validation = YamlCandidateValidation({ node ->
            calls++
            if (node == i(1)) throw SerializationException("invalid one")
            requireNotNull(node)
        }, capacity = 2)
        assertFailsWith<SerializationException> { validation.evaluate(i(1)) }
        validation.evaluate(i(2)); validation.evaluate(i(3))
        assertFailsWith<SerializationException> { validation.evaluate(i(1)) }
        assertEquals(4, calls)
    }

    @Test fun manyDifferentCandidatesAreAllEvaluatedWithoutAFalseCacheHit() {
        var calls = 0
        val validation = YamlCandidateValidation({ root -> calls++; requireNotNull(root) })
        repeat(100) { index ->
            val root: YamlNode = stringMappingOf("value" to i(index))
            assertEquals(root, validation.evaluate(root))
        }
        assertEquals(100, calls)
        assertEquals(stringMappingOf("value" to i(0)), validation.evaluate(stringMappingOf("value" to i(0))))
        assertEquals(101, calls)
    }
}
