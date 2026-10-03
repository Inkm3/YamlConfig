package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.validation.YamlCandidateValidation
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class YamlCandidateValidationTest {
    @Test fun sameReferenceAndSeparateExactTreesReuseOneEvaluation() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(calls) })
        val root = stringMappingOf("x" to sequenceOf(i(1), n()))
        assertEquals(i(1), validation.evaluate(root))
        assertEquals(i(1), validation.evaluate(root))
        assertEquals(i(1), validation.evaluate(stringMappingOf("x" to sequenceOf(i(1), n()))))
        assertEquals(1, calls)
    }

    @Test fun differentMappingOrderDoesNotReuseOrderInsensitiveNodeEquality() {
        var calls = 0
        val first = stringMappingOf("a" to i(1), "b" to i(2))
        val second = stringMappingOf("b" to i(2), "a" to i(1))
        assertEquals(first, second) // Document why a normal Map<YamlNode, ...> is unsafe.
        val validation = YamlCandidateValidation({ calls++; i(calls) })
        assertEquals(i(1), validation.evaluate(first))
        assertEquals(i(2), validation.evaluate(second))
        assertEquals(2, calls)
    }

    @Test fun mappingOrderIsObservedEvenInsideNestedLists() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(calls) })
        val a = stringMappingOf("a" to i(1), "b" to i(2))
        val b = stringMappingOf("b" to i(2), "a" to i(1))
        validation.evaluate(stringMappingOf("nested" to sequenceOf(a)))
        validation.evaluate(stringMappingOf("nested" to sequenceOf(b)))
        assertEquals(2, calls)
    }

    @Test fun missingNullKindLexemeAndContainerShapeAreDistinct() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(0) })
        val roots: List<YamlNode?> = listOf(null, n(), s("null"), i(1), s("1"),
            YamlScalarNode("+1", YamlScalarKind.INTEGER), stringMappingOf(), sequenceOf())
        roots.forEach { validation.evaluate(it) }
        assertEquals(roots.size, calls)
        roots.forEach { validation.evaluate(it) }
        assertEquals(roots.size, calls)
    }

    @Test fun changedDescendantOrSequenceOrderRequiresNewEvaluation() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(0) })
        validation.evaluate(sequenceOf(i(1), i(2)))
        validation.evaluate(sequenceOf(i(2), i(1)))
        validation.evaluate(sequenceOf(i(2), i(3)))
        assertEquals(3, calls)
    }

    @Test fun equalHashDoesNotMeanEqualInput() {
        var calls = 0
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val validation = YamlCandidateValidation({ calls++; i(calls) })
        assertEquals(i(1), validation.evaluate(s("Aa")))
        assertEquals(i(2), validation.evaluate(s("BB")))
    }

    @Test fun serializationFailureRetainsOriginalExceptionAndCause() {
        var calls = 0
        val cause = IllegalArgumentException("bad scalar")
        val problem = SerializationException("invalid", cause)
        val validation = YamlCandidateValidation({ calls++; throw problem })
        repeat(2) {
            val failure = assertFailsWith<SerializationException> { validation.evaluate(s("bad")) }
            assertSame(problem, failure)
            assertSame(cause, failure.cause)
        }
        assertEquals(1, calls)
    }

    @Test fun unexpectedFailuresAreNotSwallowedOrMemoized() {
        var calls = 0
        val problem = IllegalStateException("application failure")
        val validation = YamlCandidateValidation({ calls++; throw problem })
        repeat(2) { assertSame(problem, assertFailsWith<IllegalStateException> { validation.evaluate(i(1)) }) }
        assertEquals(2, calls)
    }

    @Test fun oldResultCanBeReusedAfterEvaluatingAnotherCandidate() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(calls) })
        assertEquals(i(1), validation.evaluate(i(1)))
        assertEquals(i(2), validation.evaluate(i(2)))
        assertEquals(i(1), validation.evaluate(i(1)))
        assertEquals(2, calls)
    }

    @Test fun capacityEvictsOldInputsInsteadOfGrowingWithoutBound() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(calls) }, capacity = 2)
        validation.evaluate(i(1)); validation.evaluate(i(2)); validation.evaluate(i(3))
        assertEquals(i(4), validation.evaluate(i(1)))
        assertEquals(4, calls)
    }

    @Test fun capacityOneOnlyRetainsTheLastEvaluation() {
        var calls = 0
        val validation = YamlCandidateValidation({ calls++; i(calls) }, capacity = 1)
        validation.evaluate(i(1)); validation.evaluate(i(2)); validation.evaluate(i(1))
        assertEquals(3, calls)
    }

    @Test fun invalidCapacityFailsImmediately() {
        assertFailsWith<IllegalArgumentException> { YamlCandidateValidation({ i(1) }, capacity = 0) }
    }
}
