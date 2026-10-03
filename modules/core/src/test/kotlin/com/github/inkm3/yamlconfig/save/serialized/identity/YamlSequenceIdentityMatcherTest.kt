package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.identity.YamlSequenceIdentityMatcher
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YamlSequenceIdentityMatcherTest {
    @Serializable private data class Item(val id: String?, val value: Int = 0)
    @Serializable private data class Numeric(val id: Int, val value: Int = 0)
    @Serializable private data class Floating(val id: Double, val value: Int = 0)
    @Serializable private enum class Code { A, B }
    @Serializable private data class EnumItem(val id: Code, val value: Int = 0)
    private fun node(id: YamlNode, value: Int = 0) = stringMappingOf("id" to id, "value" to i(value))
    private fun matcher(before: List<YamlNode>, after: List<YamlNode>) = assertNotNull(
        YamlSequenceIdentityMatcher.create(serializer<Item>().descriptor, before, after),
    )

    @Test fun sameUniqueIdentityMatchesDespiteDifferentContentAndPosition() {
        val a = node(s("a")); val before = node(s("b"), 1); val after = node(s("b"), 2)
        assertTrue(matcher(listOf(a, before), listOf(after, a)).matches(before, after))
    }
    @Test fun changedIdentityNeverMatches() {
        val before = node(s("a")); val after = node(s("b"))
        assertFalse(matcher(listOf(before), listOf(after)).matches(before, after))
    }
    @Test fun duplicatesOnEitherSideDisableOnlyThatIdentity() {
        val a = node(s("a")); val b1 = node(s("b"), 1); val b2 = node(s("b"), 2); val b3 = node(s("b"), 3)
        val oldDuplicate = matcher(listOf(a, b1, b2), listOf(b3, a))
        assertFalse(oldDuplicate.matches(b1, b3)); assertTrue(oldDuplicate.matches(a, a))
        val newDuplicate = matcher(listOf(a, b1), listOf(b2, a, b3))
        assertFalse(newDuplicate.matches(b1, b2)); assertTrue(newDuplicate.matches(a, a))
    }
    @Test fun repeatedReferenceStillCountsAsTwoOccurrences() {
        val before = node(s("b")); val after = node(s("b"), 1)
        assertFalse(matcher(listOf(before, before), listOf(after)).matches(before, after))
    }
    @Test fun nullMissingAndNonScalarValuesAreNeverIdentities() {
        for (before in listOf(node(n()), stringMappingOf("value" to i(0)), node(sequenceOf(s("a"))))) {
            assertFalse(matcher(listOf(before), listOf(before)).matches(before, before))
        }
    }
    @Test fun validEmptyStringIsNotConfusedWithMissing() {
        val before = node(s("")); val after = node(s(""), 1)
        assertTrue(matcher(listOf(before), listOf(after)).matches(before, after))
    }
    @Test fun integerSpellingsNormalizeAndNormalizedDuplicatesStayAmbiguous() {
        val before = node(YamlScalarNode("+1", YamlScalarKind.INTEGER)); val after = node(i(1), 2)
        val make = { old: List<YamlNode> -> assertNotNull(
            YamlSequenceIdentityMatcher.create(serializer<Numeric>().descriptor, old, listOf(after)),
        ) }
        assertTrue(make(listOf(before)).matches(before, after))
        assertFalse(make(listOf(before, node(i(1), 5))).matches(before, after))
    }
    @Test fun invalidKindDoesNotCoerceAStringIntoANumericIdentity() {
        val before = node(s("1")); val after = node(i(1))
        assertFalse(assertNotNull(YamlSequenceIdentityMatcher.create(serializer<Numeric>().descriptor,
            listOf(before), listOf(after))).matches(before, after))
    }
    @Test fun nonFiniteFloatingIdentityIsRejected() {
        for (text in listOf(".nan", ".inf", "-.inf")) {
            val before = node(YamlScalarNode(text, YamlScalarKind.FLOAT))
            val after = node(YamlScalarNode(text, YamlScalarKind.FLOAT), 1)
            assertFalse(assertNotNull(YamlSequenceIdentityMatcher.create(serializer<Floating>().descriptor,
                listOf(before), listOf(after))).matches(before, after))
        }
    }
    @Test fun enumIdentityUsesTheSerializedEnumNames() {
        val before = node(s("A")); val after = node(s("A"), 1); val bad = node(s("invalid"))
        val match = assertNotNull(YamlSequenceIdentityMatcher.create(serializer<EnumItem>().descriptor,
            listOf(before, bad), listOf(after, bad)))
        assertTrue(match.matches(before, after)); assertFalse(match.matches(bad, bad))
    }
}
