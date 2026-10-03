package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.nullNode
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.roundTrip
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YamlScalarNodeCodecTest {
    @Serializable
    private enum class Mode { @SerialName("on") ENABLED, DISABLED }

    @Test
    fun stringsAndCharsRoundTripWithoutLexicalCoercion() {
        for (value in listOf("", "null", "true", "123", "hello\nworld", "\u65e5\u672c\u8a9e")) {
            roundTrip(value, string(value))
        }
        roundTrip('x', string("x"))
        roundTrip('\u65e5', string("\u65e5"))
    }

    @Test
    fun booleansRoundTripAndAcceptCoreSpellings() {
        roundTrip(true, YamlScalarNode("true", YamlScalarKind.BOOLEAN))
        roundTrip(false, YamlScalarNode("false", YamlScalarKind.BOOLEAN))
        assertEquals(true, YamlSerialization.Default.decodeFromNode<Boolean>(
            YamlScalarNode("TRUE", YamlScalarKind.BOOLEAN),
        ))
    }

    @Test
    fun signedIntegerTypesUseIntegerNodes() {
        roundTrip(12.toByte(), integer(12))
        roundTrip((-300).toShort(), integer(-300))
        roundTrip(30000, integer(30000))
        roundTrip(5_000_000_000L, integer("5000000000"))
    }

    @Test
    fun nullableRootDistinguishesNullFromStringNull() {
        roundTrip<String?>(null, nullNode())
        roundTrip<String?>("null", string("null"))
        roundTrip<Int?>(42, integer(42))
        for (lexeme in listOf("", "~", "null", "Null", "NULL")) {
            assertNull(YamlSerialization.Default.decodeFromNode<String?>(
                YamlScalarNode(lexeme, YamlScalarKind.NULL),
            ))
        }
    }

    @Test
    fun invalidNullLexemeIsRejected() {
        assertFailsWith<SerializationException> {
            YamlSerialization.Default.decodeFromNode<String?>(YamlScalarNode("invalid", YamlScalarKind.NULL))
        }
    }

    @Test
    fun enumUsesSerialNameRatherThanKotlinName() {
        roundTrip(Mode.ENABLED, string("on"))
        assertFailsWith<SerializationException> {
            YamlSerialization.Default.decodeFromNode<Mode>(string("ENABLED"))
        }
    }

    @Test
    fun wrongScalarKindsAreRejectedWithoutStringCoercion() {
        assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<Int>(string("1")) }
        assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<String>(integer(1)) }
        assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<Int>(nullNode()) }
        assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<Boolean>(string("true")) }
    }

    @Test
    fun charRequiresExactlyOneCodeUnit() {
        for (value in listOf("", "ab", "\uD83D\uDE00")) {
            assertFailsWith<SerializationException> { YamlSerialization.Default.decodeFromNode<Char>(string(value)) }
        }
    }

    @Test
    fun invalidBooleanIsRejected() {
        for (value in listOf("yes", "1", "tRuE")) {
            assertFailsWith<SerializationException> {
                YamlSerialization.Default.decodeFromNode<Boolean>(YamlScalarNode(value, YamlScalarKind.BOOLEAN))
            }
        }
    }
}
