package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.testsupport.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.*

class MinimalReductionEquivalenceTest {
    @Serializable private data class Three(val a: Int = 0, val b: Int = 0, val c: Int = 0)
    @Serializable private data class Child(val a: Int = 0)
    @Serializable private data class Mixed(val data: Map<String, Int>, val obj: Child, val list: List<Child>)

    private fun compare(case: ReductionCase): ReductionRun {
        val original = case.initial.toString()
        val old = ReductionFixtures.run(case, true, true)
        val new = ReductionFixtures.run(case, false, true)
        assertEquals(old.observations, new.observations, case.name)
        assertEquals(old.candidate.root.toString(), new.candidate.root.toString(), case.name)
        assertEquals(old.candidate.patches.size, new.candidate.patches.size, case.name)
        val editor = TestYamlEditor(case.initial)
        val control = TestYamlEditor(case.initial)
        new.candidate.patches.zip(old.candidate.patches).forEach { (change, before) ->
            change.apply(editor); before.apply(control)
            assertEquals(control.root.toString(), editor.root.toString(), case.name)
        }
        assertEquals(new.candidate.root.toString(), editor.root.toString())
        assertEquals(original, case.initial.toString())
        return new
    }

    @Test fun representativeCandidateOrderAndReplayAreUnchanged() {
        ReductionFixtures.cases().forEach(::compare)
    }

    @Test fun allAcceptancePoliciesOnThreePropertiesHaveIdenticalTraces() {
        val descriptor = serializer<Three>().descriptor
        val initial = stringMappingOf("c" to i(0), "a" to i(0), "b" to i(0))
        repeat(512) { policy ->
            compare(ReductionCase("policy-$policy", descriptor, initial) { root ->
                val state = if (root !is YamlMappingNode) 8 else
                    (if (root["a"] != null) 1 else 0) or (if (root["b"] != null) 2 else 0) or
                        (if (root["c"] != null) 4 else 0)
                policy and (1 shl state) != 0
            })
        }
    }

    @Test fun rejectedAncestorIsRevisitedAfterChildRemoval() {
        val descriptor = serializer<Three>().descriptor
        val result = compare(ReductionCase("dependency", descriptor, stringMappingOf("a" to i(0), "b" to i(0))) {
            val root = it as? YamlMappingNode
            root != null && (root["a"] != null || root["b"] == null)
        })
        assertEquals(stringMappingOf(), result.candidate.root)
    }

    @Test fun unknownKeysSurviveAndCollectionsRemainWholeCandidates() {
        val shared = stringMappingOf("a" to i(0), "extra" to i(1))
        val initial = stringMappingOf("data" to shared, "obj" to shared, "list" to sequenceOf(shared))
        val result = compare(ReductionCase("same-node-different-types", serializer<Mixed>().descriptor, initial) { true })
        assertEquals(stringMappingOf("obj" to stringMappingOf("extra" to i(1)), "list" to sequenceOf(shared)), result.candidate.root)
    }

    @Test fun nonStringUnknownKeyCannotDisappearWithItsParent() {
        val initial = YamlMappingNode(linkedMapOf(YamlMapKey("1", YamlScalarKind.INTEGER) to s("keep"), YamlMapKey("a") to i(1)))
        val result = compare(ReductionCase("non-string", serializer<Three>().descriptor, initial) { true })
        assertEquals(YamlMappingNode(mapOf(YamlMapKey("1", YamlScalarKind.INTEGER) to s("keep"))), result.candidate.root)
    }

    @Test fun missingAndExplicitNullAreNotConflated() {
        val descriptor = serializer<Int?>().descriptor
        assertEquals(0, compare(ReductionCase("missing", descriptor, null) { true }).checks)
        assertEquals(1, compare(ReductionCase("explicit-null", descriptor, n()) { true }).checks)
    }

    @Test fun removingParentDoesNotVisitItsNowAbsentDescendants() {
        val result = compare(ReductionFixtures.deep(12, true))
        assertNull(result.candidate.root)
        assertEquals(1, result.checks)
        assertEquals(1, result.candidate.patches.size)
    }

    @Test fun callbacksFailWithTheOriginalException() {
        val problem = IllegalStateException("validation failed")
        val base = ReductionFixtures.wide(4, true)
        for (reference in listOf(true, false)) {
            assertSame(problem, assertFailsWith<IllegalStateException> {
                ReductionFixtures.run(base.copy(accepts = { throw problem }), reference)
            })
        }
    }
}
