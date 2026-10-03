package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlMinimalOverrideReducer
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.test.*

class YamlMinimalScalarReductionTest {
    @Test fun acceptedScalarRemovalHasOneValidationAndOneReplayablePatch() {
        for (node in listOf(i(1), s("text"), n(), YamlScalarNode("TRUE", YamlScalarKind.BOOLEAN))) {
            var calls = 0
            val result = YamlMinimalOverrideReducer.reduceToResult(serializer<String?>().descriptor, node) {
                calls++
                assertNull(it)
                true
            }
            assertEquals(1, calls)
            assertNull(result.root)
            assertEquals(1, result.removals.size)
            assertNull(result.removals.single().applyTo(node))
        }
    }

    @Test fun rejectedScalarRetainsOriginalNodeWithoutAPatch() {
        val initial = i(1)
        var calls = 0
        val result = YamlMinimalOverrideReducer.reduceToResult(serializer<Int>().descriptor, initial) {
            calls++
            assertNull(it)
            false
        }
        assertEquals(1, calls)
        assertSame(initial, result.root)
        assertTrue(result.removals.isEmpty())
    }

    @Test fun scalarValidatorFailurePropagatesUnchanged() {
        val error = SerializationException("required root")
        assertSame(error, assertFailsWith<SerializationException> {
            YamlMinimalOverrideReducer.reduceToResult(serializer<Int>().descriptor, i(1)) { throw error }
        })
    }
}
