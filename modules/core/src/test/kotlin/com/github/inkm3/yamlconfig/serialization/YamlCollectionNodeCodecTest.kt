package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.nullNode
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.roundTrip
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.sequence
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlCollectionNodeCodecTest {
    private val format = YamlSerialization.Default

    @Test
    fun listOfScalarsRoundTrips() {
        roundTrip(listOf("lobby", "creative"), sequence(string("lobby"), string("creative")))
        roundTrip(listOf(1, -2), sequence(integer(1), integer(-2)))
    }

    @Test
    fun emptyCollectionsArePresentEmptyNodesNotMissingValues() {
        roundTrip(emptyList<String>(), sequence())
        roundTrip(emptyMap<String, Int>(), mapping())
    }

    @Test
    fun nullableListAndElementsRemainDistinct() {
        roundTrip<List<String?>?>(null, nullNode())
        roundTrip<List<String?>?>(listOf(null, "null"), sequence(nullNode(), string("null")))
    }

    @Test
    fun listOfObjectsRoundTrips() {
        val values = listOf(CodecServer("a"), CodecServer("b", 30000))
        roundTrip(values, sequence(
            mapping("name" to string("a"), "port" to integer(25565)),
            mapping("name" to string("b"), "port" to integer(30000)),
        ))
    }

    @Test
    fun deeplyNestedCollectionsRoundTrip() {
        val value = mapOf("regions" to listOf(mapOf("ports" to listOf(1, 2)), emptyMap()))
        val node = mapping("regions" to sequence(mapping("ports" to sequence(integer(1), integer(2))), mapping()))
        roundTrip(value, node)
    }

    @Test
    fun nestedEmptyListsPreserveTheirPositions() {
        roundTrip(listOf(emptyList<Int>(), listOf(1), emptyList()), sequence(sequence(), sequence(integer(1)), sequence()))
    }

    @Test
    fun primitiveArraysFollowTheIndexedDecodingProtocol() {
        val value = intArrayOf(1, 3, 5)
        assertEquals(sequence(integer(1), integer(3), integer(5)), format.encodeToNode(value))
        assertContentEquals(value, format.decodeFromNode<IntArray>(format.encodeToNode(value)))
    }

    @Test
    fun wrongContainerTypesAreRejected() {
        assertFailsWith<SerializationException> { format.decodeFromNode<List<Int>>(mapping()) }
        assertFailsWith<SerializationException> { format.decodeFromNode<Map<String, Int>>(sequence()) }
        assertFailsWith<SerializationException> { format.decodeFromNode<CodecServer>(sequence()) }
    }

    @Test
    fun repeatedCallsDoNotReuseMutableEncoderOrDecoderState() {
        repeat(20) { index ->
            val value = CodecConfig(listOf(CodecServer("server-$index", 20000 + index)))
            assertEquals(value, format.decodeFromNode<CodecConfig>(format.encodeToNode(value)))
        }
        assertEquals(sequence(), format.encodeToNode(emptyList<Int>()))
    }
}
