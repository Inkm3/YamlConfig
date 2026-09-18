package com.github.inkm3.yamlconfig.schema.scalar.internal

import java.math.BigInteger

internal object YamlScalarLexicalCodec {

    private val decimalIntegerPattern = Regex("""[+-]?[0-9]+""")
    private val octalIntegerPattern = Regex("""0o[0-7]+""")
    private val hexadecimalIntegerPattern = Regex("""0x[0-9a-fA-F]+""")
    private val decimalFloatPattern = Regex("""[+-]?(?:\.[0-9]+|[0-9]+(?:\.[0-9]*)?)(?:[eE][+-]?[0-9]+)?""")

    private fun decodeInteger(value: String): BigInteger? {
        return try {
            when {
                decimalIntegerPattern.matches(value) -> BigInteger(value, 10)
                octalIntegerPattern.matches(value) -> BigInteger(value.substring(2), 8)
                hexadecimalIntegerPattern.matches(value) -> BigInteger(value.substring(2), 16)
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }



    internal fun decodeBoolean(value: String): Boolean? {
        return when (value) {
            "true", "True", "TRUE" -> true
            "false", "False", "FALSE" -> false

            else -> null
        }
    }

    internal fun encodeBoolean(value: Boolean): String {
        return if (value) {
            "true"
        } else {
            "false"
        }
    }

    internal fun decodeInt(value: String): Int? {
        val integer = decodeInteger(value)
            ?: return null

        return try {
            integer.intValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    internal fun encodeInt(value: Int): String {
        return value.toString()
    }

    internal fun decodeLong(value: String): Long? {
        val integer = decodeInteger(value)
            ?: return null

        return try {
            integer.longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    internal fun encodeLong(value: Long): String {
        return value.toString()
    }

    internal fun decodeDouble(value: String): Double? {
        when (value.lowercase()) {
            ".inf", "+.inf" -> return Double.POSITIVE_INFINITY
            "-.inf" -> return Double.NEGATIVE_INFINITY
            ".nan" -> return Double.NaN
        }

        if (!decimalFloatPattern.matches(value)) {
            return null
        }

        val decoded = value.toDoubleOrNull()
            ?: return null

        if (decoded.isInfinite()) {
            return null
        }

        return decoded
    }

    internal fun encodeDouble(value: Double): String {
        return when {
            value.isNaN() -> ".nan"
            value == Double.POSITIVE_INFINITY -> ".inf"
            value == Double.NEGATIVE_INFINITY -> "-.inf"
            else -> value.toString()
        }
    }


    internal fun decodeFloat(value: String): Float? {
        when (value.lowercase()) {
            ".inf", "+.inf" -> return Float.POSITIVE_INFINITY
            "-.inf" -> return Float.NEGATIVE_INFINITY
            ".nan" -> return Float.NaN
        }

        if (!decimalFloatPattern.matches(value)) {
            return null
        }

        val decoded = value.toFloatOrNull()
            ?: return null

        if (decoded.isInfinite()) {
            return null
        }

        return decoded
    }

    internal fun encodeFloat(value: Float): String {
        return when {
            value.isNaN() -> ".nan"
            value == Float.POSITIVE_INFINITY -> ".inf"
            value == Float.NEGATIVE_INFINITY -> "-.inf"
            else -> value.toString()
        }
    }


}