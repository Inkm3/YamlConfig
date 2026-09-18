package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import kotlin.test.*

class ScalarSchemaTest {
    private val root = YamlPath.root()

    @Test fun stringRoundTripAndKeyRoundTrip() {
        val schema = str()
        assertEquals("hello", schema.decode(YamlScalarNode("hello", YamlScalarKind.STRING), root))
        assertEquals(YamlScalarNode("hello", YamlScalarKind.STRING), schema.encode("hello"))
        assertEquals("hello", schema.decodeKey(YamlMapKey("hello"), root))
        assertEquals(YamlMapKey("hello"), schema.encodeKey("hello"))
    }

    @Test fun booleanAcceptsCoreLexemesAndCanonicalizes() {
        val schema = boolean()
        for (lexeme in listOf("true", "True", "TRUE")) assertTrue(schema.decode(YamlScalarNode(lexeme, YamlScalarKind.BOOLEAN), root))
        for (lexeme in listOf("false", "False", "FALSE")) assertFalse(schema.decode(YamlScalarNode(lexeme, YamlScalarKind.BOOLEAN), root))
        assertEquals(YamlScalarNode("true", YamlScalarKind.BOOLEAN), schema.encode(true))
        assertFailsWith<YamlSchemaException> { schema.decode(YamlScalarNode("yes", YamlScalarKind.BOOLEAN), root) }
    }

    @Test fun intSupportsDecimalOctalHexAndRangeChecking() {
        val schema = int()
        assertEquals(255, schema.decode(YamlScalarNode("255", YamlScalarKind.INTEGER), root))
        assertEquals(255, schema.decode(YamlScalarNode("0o377", YamlScalarKind.INTEGER), root))
        assertEquals(255, schema.decode(YamlScalarNode("0xff", YamlScalarKind.INTEGER), root))
        assertEquals(-12, schema.decode(YamlScalarNode("-12", YamlScalarKind.INTEGER), root))
        assertFailsWith<YamlSchemaException> { schema.decode(YamlScalarNode("999999999999999999999", YamlScalarKind.INTEGER), root) }
        assertEquals(YamlScalarNode("255", YamlScalarKind.INTEGER), schema.encode(255))
    }

    @Test fun longSupportsLargeValuesAndRejectsOverflow() {
        val schema = long()
        assertEquals(Int.MAX_VALUE.toLong() + 1, schema.decode(YamlScalarNode("2147483648", YamlScalarKind.INTEGER), root))
        assertEquals(255L, schema.decode(YamlScalarNode("0xff", YamlScalarKind.INTEGER), root))
        assertFailsWith<YamlSchemaException> { schema.decode(YamlScalarNode("9223372036854775808", YamlScalarKind.INTEGER), root) }
    }

    @Test fun floatAndDoubleSupportSpecialValuesAndCanonicalEncoding() {
        val floatSchema = float()
        val doubleSchema = double()
        assertEquals(0.5f, floatSchema.decode(YamlScalarNode(".5", YamlScalarKind.FLOAT), root))
        assertEquals(1000.0, doubleSchema.decode(YamlScalarNode("1e3", YamlScalarKind.FLOAT), root))
        assertEquals(Double.POSITIVE_INFINITY, doubleSchema.decode(YamlScalarNode("+.inf", YamlScalarKind.FLOAT), root))
        assertTrue(doubleSchema.decode(YamlScalarNode(".NaN", YamlScalarKind.FLOAT), root).isNaN())
        assertEquals(YamlScalarNode(".inf", YamlScalarKind.FLOAT), doubleSchema.encode(Double.POSITIVE_INFINITY))
        assertEquals(YamlScalarNode("-.inf", YamlScalarKind.FLOAT), floatSchema.encode(Float.NEGATIVE_INFINITY))
        assertEquals(YamlScalarNode(".nan", YamlScalarKind.FLOAT), doubleSchema.encode(Double.NaN))
    }

    @Test fun finiteFloatOverflowIsRejected() {
        assertFailsWith<YamlSchemaException> {
            double().decode(YamlScalarNode("1e999999", YamlScalarKind.FLOAT), root)
        }
    }

    @Test fun nanValuesAreEquivalent() {
        assertTrue(double().equivalent(Double.NaN, Double.NaN))
        assertTrue(float().equivalent(Float.NaN, Float.NaN))
    }

    @Test fun wrongScalarKindIsRejectedWithPath() {
        val path = root.child("port")
        val error = assertFailsWith<YamlSchemaException> {
            int().decode(YamlScalarNode("1", YamlScalarKind.STRING), path)
        }
        assertEquals(path, error.path)
    }

    @Test fun nonScalarNodeIsRejected() {
        assertFailsWith<YamlSchemaException> { str().decode(YamlSequenceNode(emptyList()), root) }
    }
}
