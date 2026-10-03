package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.sequence
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YamlCodecErrorPathTest {
    @Serializable
    private data class MapConfig(val ports: Map<String, Int>)

    @Test
    fun invalidScalarInListObjectReportsItsFullPath() {
        val node = mapping("servers" to sequence(mapping("name" to string("a"), "port" to string("abc"))))
        val error = assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<CodecConfig>(node) }
        assertTrue(error.message.orEmpty().endsWith(" at $.servers[0].port"), error.message)
    }

    @Test
    fun nestedUnknownPropertyReportsItsOwnPath() {
        val node = mapping("servers" to sequence(mapping("name" to string("a"), "future" to integer(1))))
        val error = assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<CodecConfig>(node) }
        assertTrue(error.message.orEmpty().endsWith(" at $.servers[0].future"), error.message)
    }

    @Test
    fun missingRequiredFieldKeepsCauseAndContainingObjectPath() {
        val node = mapping("servers" to sequence(mapping("port" to integer(1))))
        val error = assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<CodecConfig>(node) }
        assertNotNull(error.cause)
        assertTrue(error.message.orEmpty().contains("name"), error.message)
        assertTrue(error.message.orEmpty().endsWith(" at $.servers[0]"), error.message)
    }

    @Test
    fun mapValueErrorsUseTheKeyPathAndEscapeSpecialNames() {
        val key = "a.b\n\"c"
        val node = mapping("ports" to mapping(key to string("bad")))
        val error = assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<MapConfig>(node) }
        val expected = YamlPath.root().child("ports").child(key)
        assertTrue(error.message.orEmpty().endsWith(" at $expected"), error.message)
    }

    @Test
    fun errorMessagesDoNotAccumulateMultipleOuterPathSuffixes() {
        val node = mapping("servers" to sequence(mapping("name" to string("a"), "port" to string("bad"))))
        val error = assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<CodecConfig>(node) }
        assertEquals(1, Regex(" at ").findAll(error.message.orEmpty()).count())
    }
}
