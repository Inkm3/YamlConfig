package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidatePlanner
import com.github.inkm3.yamlconfig.testsupport.i
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YamlSaveValidationScopeTest {
    @Test fun eachPlanStartsWithAnEmptyMemoEvenOnTheSamePlanner() {
        val case = SaveProfileFixtures.cases().first { it.name == "large-unrepresentable" }
        var calls = 0
        val planner = YamlSaveCandidatePlanner(case.descriptor) { calls++; case.normalize(it) }
        assertFailsWith<SerializationException> { planner.plan(case.baseline, case.expected, case.user, case.mode) }
        val firstCalls = calls
        assertTrue(firstCalls > 0)
        assertFailsWith<SerializationException> { planner.plan(case.baseline, case.expected, case.user, case.mode) }
        assertEquals(firstCalls * 2, calls)
    }

    @Test fun actualEditorValidationIsAlwaysFreshAfterCandidatePlanning() {
        var calls = 0
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { calls++; requireNotNull(it) }
        val candidate = planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES)
        assertEquals(1, calls)
        planner.requireMatches(candidate.root, i(2))
        planner.requireMatches(candidate.root, i(2))
        assertEquals(3, calls)
    }

    @Test fun finalValidationCannotHideADifferentEditorValue() {
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { requireNotNull(it) }
        planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES)
        assertFailsWith<SerializationException> { planner.requireMatches(i(99), i(2)) }
    }

    @Test fun sameNodeCannotReuseResultsAcrossDifferentValidationCalls() {
        var actual = i(2)
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { actual }
        planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES)
        actual = i(3)
        assertFailsWith<SerializationException> { planner.requireMatches(i(2), i(2)) }
        assertFailsWith<SerializationException> {
            planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES)
        }
    }

    @Test fun unrepresentableOmissionStillFailsWithFewerFullReloads() {
        for (case in SaveProfileFixtures.cases().filter { it.name.endsWith("unrepresentable") }) {
            val reference = case.run(true)
            val optimized = case.run(false)
            assertNotNull(optimized.error)
            assertEquals(reference.error?.message, optimized.error.message)
            assertTrue(optimized.reloads < reference.reloads,
                "${case.name}: ${optimized.reloads} must be less than ${reference.reloads}")
        }
    }

    @Test fun singlePassSuccessStillPerformsExactlyOneReload() {
        val case = SaveProfileFixtures.cases().first { it.name == "scalar-preserve" }
        assertEquals(1, case.run(false).reloads)
    }
}
