package com.github.inkm3.yamlconfig.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class YamlMalformedSerializerTest {
    @Test
    fun multipleRootValuesAreRejected() {
        val serializer = object : SerializationStrategy<Int> {
            override val descriptor = Int.serializer().descriptor
            override fun serialize(encoder: Encoder, value: Int) {
                encoder.encodeInt(value)
                encoder.encodeInt(value)
            }
        }
        assertFailsWith<SerializationException> { YamlSerialization.Default.encodeToNode(serializer, 1) }
    }

    @Test
    fun missingRootValueIsRejected() {
        val serializer = object : SerializationStrategy<Int> {
            override val descriptor = Int.serializer().descriptor
            override fun serialize(encoder: Encoder, value: Int) = Unit
        }
        assertFailsWith<SerializationException> { YamlSerialization.Default.encodeToNode(serializer, 1) }
    }

    @Test
    fun mapKeyWithoutValueIsRejected() {
        val serializer = object : SerializationStrategy<Unit> {
            override val descriptor = MapSerializer(Int.serializer(), String.serializer()).descriptor
            override fun serialize(encoder: Encoder, value: Unit) {
                val composite = encoder.beginCollection(descriptor, 1)
                composite.encodeIntElement(descriptor, 0, 1)
                composite.endStructure(descriptor)
            }
        }
        assertFailsWith<SerializationException> { YamlSerialization.Default.encodeToNode(serializer, Unit) }
    }

    @Test
    fun outOfOrderListIndicesAreRejected() {
        val serializer = object : SerializationStrategy<Unit> {
            override val descriptor = ListSerializer(Int.serializer()).descriptor
            override fun serialize(encoder: Encoder, value: Unit) {
                val composite = encoder.beginCollection(descriptor, 1)
                composite.encodeIntElement(descriptor, 1, 42)
                composite.endStructure(descriptor)
            }
        }
        assertFailsWith<SerializationException> { YamlSerialization.Default.encodeToNode(serializer, Unit) }
    }
}
