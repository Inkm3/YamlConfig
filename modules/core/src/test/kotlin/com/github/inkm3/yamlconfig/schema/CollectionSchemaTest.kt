package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlin.test.*

class CollectionSchemaTest {
    private val root = YamlPath.root()

    @Test fun listDecodesEncodesAndComparesElementWise() {
        val schema = list(int())
        val node = sequenceOf(i(1), i(2), i(3))
        assertEquals(listOf(1, 2, 3), schema.decode(node, root))
        assertEquals(node, schema.encode(listOf(1, 2, 3)))
        assertTrue(schema.equivalent(listOf(1, 2), listOf(1, 2)))
        assertFalse(schema.equivalent(listOf(1, 2), listOf(2, 1)))
    }

    @Test fun listReportsElementPathOnDecodeFailure() {
        val error = assertFailsWith<YamlSchemaException> {
            list(int()).decode(sequenceOf(i(1), s("bad")), root.child("values"))
        }
        assertEquals(root.child("values").child(1), error.path)
    }

    @Test fun mapSupportsTypedIntegerKeysAndPreservesIterationOrder() {
        val schema = map(int(), str())
        val node = YamlMappingNode(linkedMapOf(
            YamlMapKey("0xff", YamlScalarKind.INTEGER) to s("hex"),
            YamlMapKey("2", YamlScalarKind.INTEGER) to s("two"),
        ))
        val decoded = schema.decode(node, root)
        assertEquals(listOf(255, 2), decoded.keys.toList())
        assertEquals("hex", decoded[255])
        assertEquals(YamlMappingNode(linkedMapOf(
            YamlMapKey("255", YamlScalarKind.INTEGER) to s("hex"),
            YamlMapKey("2", YamlScalarKind.INTEGER) to s("two"),
        )), schema.encode(decoded))
    }

    @Test fun mapRejectsDuplicateKotlinKeysAfterLexicalDecode() {
        val node = YamlMappingNode(linkedMapOf(
            YamlMapKey("1", YamlScalarKind.INTEGER) to s("a"),
            YamlMapKey("01", YamlScalarKind.INTEGER) to s("b"),
        ))
        val error = assertFailsWith<YamlSchemaException> { map(int(), str()).decode(node, root) }
        assertEquals(root.child(YamlMapKey("01", YamlScalarKind.INTEGER)), error.path)
    }

    @Test fun mapEquivalentIgnoresIterationOrderButComparesValuesBySchema() {
        val schema = map(str(), double())
        val first = linkedMapOf("a" to Double.NaN, "b" to 2.0)
        val second = linkedMapOf("b" to 2.0, "a" to Double.NaN)
        assertTrue(schema.equivalent(first, second))
        assertFalse(schema.equivalent(first, mapOf("a" to Double.NaN, "b" to 3.0)))
    }

    @Test fun collectionKindMismatchesAreRejected() {
        assertFailsWith<YamlSchemaException> { list(str()).decode(stringMappingOf(), root) }
        assertFailsWith<YamlSchemaException> { map(str(), str()).decode(sequenceOf(), root) }
    }
}
