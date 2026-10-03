package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.sequence
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class YamlSerializationOptionsTest {
    @Serializable
    private data class Defaults(val port: Int = 25565)

    @Serializable
    private data class Annotated(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) val always: Int = 1,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val never: Int = 2,
        val normal: Int = 3,
    )

    @Test
    fun defaultValuedPropertyIsIncludedByDefault() {
        assertEquals(mapping("port" to integer(25565)), YamlSerialization.Default.encodeToNode(Defaults()))
    }

    @Test
    fun encodeDefaultsFalseOmitsOnlyDefaultValuedProperties() {
        val format = YamlSerialization(encodeDefaults = false)
        assertEquals(mapping(), format.encodeToNode(Defaults()))
        assertEquals(mapping("port" to integer(30000)), format.encodeToNode(Defaults(30000)))
        assertEquals(Defaults(), format.decodeFromNode<Defaults>(mapping()))
    }

    @Test
    fun encodeDefaultAnnotationsOverrideTheFormatOption() {
        assertEquals(mapping("always" to integer(1)), YamlSerialization(encodeDefaults = false).encodeToNode(Annotated()))
        assertEquals(mapping("always" to integer(1), "normal" to integer(3)), YamlSerialization.Default.encodeToNode(Annotated()))
    }

    @Test
    fun neverAnnotationDoesNotDropNonDefaultValues() {
        val value = Annotated(never = 7)
        assertEquals(value, YamlSerialization.Default.decodeFromNode<Annotated>(YamlSerialization.Default.encodeToNode(value)))
    }

    @Test
    fun unknownObjectPropertyFailsByDefault() {
        assertFailsWith<SerializationException> {
            YamlSerialization.Default.decodeFromNode<CodecServer>(mapping("name" to string("a"), "extra" to string("x")))
        }
    }

    @Test
    fun ignoreUnknownKeysAppliesRecursivelyWithoutMutatingNodes() {
        val node = mapping("servers" to sequence(mapping("name" to string("a"), "extra" to integer(9))), "future" to mapping())
        val before = node.toString()
        val format = YamlSerialization(ignoreUnknownKeys = true)
        assertEquals(CodecConfig(listOf(CodecServer("a"))), format.decodeFromNode<CodecConfig>(node))
        assertEquals(before, node.toString())
    }

    @Test
    fun ignoreUnknownKeysDoesNotSuppressInvalidKnownValues() {
        assertFailsWith<SerializationException> {
            YamlSerialization(ignoreUnknownKeys = true).decodeFromNode<CodecServer>(
                mapping("name" to string("a"), "port" to string("invalid")),
            )
        }
    }

    @Test
    fun mapEntriesAreDataAndNotUnknownObjectProperties() {
        val node = mapping("future-option" to integer(1))
        assertEquals(mapOf("future-option" to 1), YamlSerialization.Default.decodeFromNode<Map<String, Int>>(node))
    }

    @Test
    fun objectKeysMustBeStringsEvenWithIgnoreUnknownKeys() {
        val node = YamlMappingNode(mapOf(YamlMapKey("1", YamlScalarKind.INTEGER) to string("a")))
        assertFailsWith<SerializationException> { YamlSerialization(ignoreUnknownKeys = true).decodeFromNode<Defaults>(node) }
    }
}
