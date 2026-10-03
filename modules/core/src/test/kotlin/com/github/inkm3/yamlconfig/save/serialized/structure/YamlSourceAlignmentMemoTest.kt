package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignment
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignmentMemo
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class YamlSourceAlignmentMemoTest {
    @Serializable private data class Optional(val value: Int = 1)
    private fun source() = sequenceOf(YamlScalarNode("+1", YamlScalarKind.INTEGER))

    @Test fun identicalCollectionPairIsTraversedOnlyOnce() {
        var calls = 0
        val memo = YamlSourceAlignmentMemo { d, b, u, o -> calls++; YamlSourceAlignment.matches(d, b, u, o) }
        val d = serializer<List<Int>>().descriptor
        val b = sequenceOf(i(1)); val u = source()
        repeat(20) { assertTrue(memo.matches(d, b, u)) }
        assertEquals(1, calls)
    }

    @Test fun negativeResultsAreReusedWithoutConflatingOmissionPolicy() {
        var calls = 0
        val memo = YamlSourceAlignmentMemo { d, b, u, o -> calls++; YamlSourceAlignment.matches(d, b, u, o) }
        val d = serializer<Optional>().descriptor
        val b = stringMappingOf(); val u = stringMappingOf("value" to i(1))
        repeat(3) { assertFalse(memo.matches(d, b, u)); assertTrue(memo.matches(d, b, u, true)) }
        assertEquals(2, calls)
    }

    @Test fun differentDescriptorsWithTheSameNameNeverShareAResult() {
        val integer = buildClassSerialDescriptor("same.Name") { element("value", serializer<Int>().descriptor) }
        val string = buildClassSerialDescriptor("same.Name") { element("value", serializer<String>().descriptor) }
        val b = stringMappingOf("value" to i(1))
        val u = stringMappingOf("value" to YamlScalarNode("+1", YamlScalarKind.INTEGER))
        val memo = YamlSourceAlignmentMemo()
        assertTrue(memo.matches(integer, b, u))
        assertFalse(memo.matches(string, b, u))
    }

    @Test fun distinctSourceReferencesCannotReuseEvenEqualContent() {
        var calls = 0
        val memo = YamlSourceAlignmentMemo { d, b, u, o -> calls++; YamlSourceAlignment.matches(d, b, u, o) }
        val d = serializer<List<Int>>().descriptor
        val b = sequenceOf(i(1))
        repeat(3) { assertTrue(memo.matches(d, b, source())) }
        assertEquals(3, calls)
    }

    @Test fun boundedEvictionReevaluatesRemovedEntries() {
        var calls = 0
        val memo = YamlSourceAlignmentMemo(1) { d, b, u, o -> calls++; YamlSourceAlignment.matches(d, b, u, o) }
        val d = serializer<List<Int>>().descriptor
        val b = sequenceOf(i(1)); val u1 = source(); val u2 = source()
        memo.matches(d, b, u1); memo.matches(d, b, u2); memo.matches(d, b, u1)
        assertEquals(3, calls)
    }

    @Test fun exceptionsAreNotSwallowedOrCached() {
        val error = IllegalStateException("broken descriptor")
        var calls = 0
        val memo = YamlSourceAlignmentMemo { _, _, _, _ -> calls++; throw error }
        val b = sequenceOf(i(1)); val u = source()
        repeat(2) {
            assertSame(error, assertFailsWith<IllegalStateException> { memo.matches(serializer<List<Int>>().descriptor, b, u) })
        }
        assertEquals(2, calls)
    }

    @Test fun newPlanHasNoOldEntriesAndScalarChecksBypassStorage() {
        var calls = 0
        val d = serializer<Int>().descriptor
        val b = i(1); val u = YamlScalarNode("+1", YamlScalarKind.INTEGER)
        repeat(2) {
            val memo = YamlSourceAlignmentMemo { desc, before, user, o -> calls++; YamlSourceAlignment.matches(desc, before, user, o) }
            repeat(2) { assertTrue(memo.matches(d, b, u)) }
        }
        assertEquals(4, calls)
    }

    @Test fun invalidCapacityIsRejected() {
        assertFailsWith<IllegalArgumentException> { YamlSourceAlignmentMemo(0) }
    }
}
