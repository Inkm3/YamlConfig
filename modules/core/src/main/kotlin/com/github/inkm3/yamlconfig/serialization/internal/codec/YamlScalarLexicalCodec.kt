package com.github.inkm3.yamlconfig.serialization.internal.codec

import java.math.BigInteger

/** YAML 1.2 Core scalar spellings; no locale-sensitive parsing or truncation. */
internal object YamlScalarLexicalCodec {
    private val decimal = Regex("[+-]?[0-9]+")
    private val octal = Regex("0o[0-7]+")
    private val hexadecimal = Regex("0x[0-9a-fA-F]+")
    private val floating = Regex("[+-]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)(?:[eE][+-]?[0-9]+)?")

    fun decodeBoolean(value: String): Boolean? = when (value) {
        "true", "True", "TRUE" -> true
        "false", "False", "FALSE" -> false
        else -> null
    }

    fun isNull(value: String): Boolean =
        value.isEmpty() || value == "~" || value == "null" || value == "Null" || value == "NULL"

    fun decodeByte(value: String): Byte? = integer(value, BigInteger::byteValueExact)
    fun decodeShort(value: String): Short? = integer(value, BigInteger::shortValueExact)
    fun decodeInt(value: String): Int? = integer(value, BigInteger::intValueExact)
    fun decodeLong(value: String): Long? = integer(value, BigInteger::longValueExact)

    fun decodeFloat(value: String, integer: Boolean = false): Float? {
        if (!integer) {
            special(value)?.let { return it.toFloat() }
            if (!floating.matches(value)) return null
        }
        val result = if (integer) parseInteger(value)?.toFloat() else value.toFloatOrNull()
        return result?.takeIf { it.isFinite() }
    }

    fun decodeDouble(value: String, integer: Boolean = false): Double? {
        if (!integer) {
            special(value)?.let { return it }
            if (!floating.matches(value)) return null
        }
        val result = if (integer) parseInteger(value)?.toDouble() else value.toDoubleOrNull()
        return result?.takeIf { it.isFinite() }
    }

    fun encodeFloat(value: Float): String = when {
        value.isNaN() -> ".nan"
        value == Float.POSITIVE_INFINITY -> ".inf"
        value == Float.NEGATIVE_INFINITY -> "-.inf"
        else -> value.toString()
    }

    fun encodeDouble(value: Double): String = when {
        value.isNaN() -> ".nan"
        value == Double.POSITIVE_INFINITY -> ".inf"
        value == Double.NEGATIVE_INFINITY -> "-.inf"
        else -> value.toString()
    }

    private fun special(value: String): Double? = when (value) {
        ".inf", ".Inf", ".INF", "+.inf", "+.Inf", "+.INF" -> Double.POSITIVE_INFINITY
        "-.inf", "-.Inf", "-.INF" -> Double.NEGATIVE_INFINITY
        ".nan", ".NaN", ".NAN" -> Double.NaN
        else -> null
    }

    private inline fun <T> integer(value: String, convert: (BigInteger) -> T): T? {
        val parsed = parseInteger(value) ?: return null
        return try {
            convert(parsed)
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun parseInteger(value: String): BigInteger? = when {
        decimal.matches(value) -> BigInteger(value, 10)
        octal.matches(value) -> BigInteger(value.substring(2), 8)
        hexadecimal.matches(value) -> BigInteger(value.substring(2), 16)
        else -> null
    }
}
