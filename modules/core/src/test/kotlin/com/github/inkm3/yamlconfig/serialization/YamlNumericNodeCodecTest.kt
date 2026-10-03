package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.serialization.CodecFixtures.floating
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import kotlinx.serialization.SerializationException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YamlNumericNodeCodecTest {
    private val format = YamlSerialization.Default

    @Test
    fun signedBoundsRoundTripExactly() {
        for (value in listOf(Byte.MIN_VALUE, Byte.MAX_VALUE)) {
            assertEquals(value, format.decodeFromNode<Byte>(format.encodeToNode(value)))
        }
        for (value in listOf(Short.MIN_VALUE, Short.MAX_VALUE)) {
            assertEquals(value, format.decodeFromNode<Short>(format.encodeToNode(value)))
        }
        for (value in listOf(Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertEquals(value, format.decodeFromNode<Int>(format.encodeToNode(value)))
        }
        for (value in listOf(Long.MIN_VALUE, Long.MAX_VALUE)) {
            assertEquals(value, format.decodeFromNode<Long>(format.encodeToNode(value)))
        }
    }

    @Test
    fun overflowNeverTruncates() {
        for (value in listOf("128", "-129")) {
            assertFailsWith<SerializationException> { format.decodeFromNode<Byte>(integer(value)) }
        }
        for (value in listOf("32768", "-32769")) {
            assertFailsWith<SerializationException> { format.decodeFromNode<Short>(integer(value)) }
        }
        for (value in listOf("2147483648", "-2147483649")) {
            assertFailsWith<SerializationException> { format.decodeFromNode<Int>(integer(value)) }
        }
        for (value in listOf("9223372036854775808", "-9223372036854775809")) {
            assertFailsWith<SerializationException> { format.decodeFromNode<Long>(integer(value)) }
        }
    }

    @Test
    fun integerCoreLexemesDecodeToTheSameValue() {
        for (value in listOf("25565", "+25565", "025565", "0x63dd", "0o61735")) {
            assertEquals(25565, format.decodeFromNode<Int>(integer(value)), value)
        }
    }

    @Test
    fun malformedIntegerLexemesAreRejected() {
        for (value in listOf("", " 1", "1 ", "1_000", "1e3", "1.0", "0x", "0o8", "+0x1", "-0x1")) {
            assertFailsWith<SerializationException>(value) { format.decodeFromNode<Long>(integer(value)) }
        }
    }

    @Test
    fun finiteFloatingValuesAndNegativeZeroRoundTrip() {
        for (value in listOf(0.0f, -0.0f, 1.25f, Float.MIN_VALUE, Float.MAX_VALUE)) {
            assertEquals(value.toRawBits(), format.decodeFromNode<Float>(format.encodeToNode(value)).toRawBits())
        }
        for (value in listOf(0.0, -0.0, 1.25, Double.MIN_VALUE, Double.MAX_VALUE)) {
            assertEquals(value.toRawBits(), format.decodeFromNode<Double>(format.encodeToNode(value)).toRawBits())
        }
    }

    @Test
    fun specialFloatingValuesUseYamlSpellings() {
        assertEquals(floating(".nan"), format.encodeToNode(Double.NaN))
        assertEquals(floating(".inf"), format.encodeToNode(Float.POSITIVE_INFINITY))
        assertEquals(floating("-.inf"), format.encodeToNode(Double.NEGATIVE_INFINITY))
        assertTrue(format.decodeFromNode<Double>(floating(".NaN")).isNaN())
        assertTrue(format.decodeFromNode<Float>(floating(".NAN")).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, format.decodeFromNode<Double>(floating("+.INF")))
        assertEquals(Float.NEGATIVE_INFINITY, format.decodeFromNode<Float>(floating("-.Inf")))
    }

    @Test
    fun decimalFloatSyntaxIsValidatedBeforeParsing() {
        assertEquals(0.5, format.decodeFromNode<Double>(floating(".5")))
        assertEquals(1000.0, format.decodeFromNode<Double>(floating("1e3")))
        for (value in listOf("NaN", "Infinity", "0x1.0p0", "1f", " 1.0", ".", ".iNF", "1_000.0")) {
            assertFailsWith<SerializationException>(value) { format.decodeFromNode<Double>(floating(value)) }
        }
    }

    @Test
    fun finiteLexemeOverflowIsNotSilentlyConvertedToInfinity() {
        assertFailsWith<SerializationException> { format.decodeFromNode<Float>(floating("1e100")) }
        assertFailsWith<SerializationException> { format.decodeFromNode<Double>(floating("1e9999")) }
    }

    @Test
    fun integerNodesCanBeReadAsFloatingPointIncludingRadixForms() {
        assertEquals(25565.0, format.decodeFromNode<Double>(integer("0x63dd")))
        assertEquals(25565.0f, format.decodeFromNode<Float>(integer("0o61735")))
        assertEquals(1.0e20, format.decodeFromNode<Double>(integer("100000000000000000000")))
        assertFailsWith<SerializationException> { format.decodeFromNode<Int>(floating("1.0")) }
        assertFailsWith<SerializationException> { format.decodeFromNode<Float>(integer("1e3")) }
    }

    @Test
    fun randomizedSignedNumbersRoundTrip() {
        val random = Random(8317)
        repeat(500) {
            val value = random.nextLong()
            assertEquals(value, format.decodeFromNode<Long>(format.encodeToNode(value)))
            val small = random.nextInt()
            assertEquals(small, format.decodeFromNode<Int>(format.encodeToNode(small)))
        }
    }
}
