package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.load.internal.YamlConfigValueLoader
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidatePlanner
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class YamlSaveCandidatePlannerTest {
    @Serializable private data class Dependent(val base: Int = 1, val derived: Int = base + 1)

    @Test fun editsAndCandidateAgree() {
        val before = i(1)
        val after = i(2)
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { requireNotNull(it) }
        val candidate = planner.plan(before, after, before, YamlSaveMode.PRESERVE_OVERRIDES)
        assertEquals(after, candidate.root)
        assertEquals(after, candidate.patches.fold(before as com.github.inkm3.yamlconfig.node.YamlNode?) {
            node, patch -> patch.applyTo(node)
        })
    }

    @Test fun forcedRetryRepairsAnUnchangedInheritedSibling() {
        val format = YamlSerialization.Default
        val loader = YamlConfigValueLoader(serializer<Dependent>(), format)
        val user = stringMappingOf("base" to i(1))
        val planner = YamlSaveCandidatePlanner(serializer<Dependent>().descriptor) {
            format.encodeToNode(loader.load(null, it))
        }
        val candidate = planner.plan(format.encodeToNode(Dependent()), format.encodeToNode(Dependent(3, 2)),
            user, YamlSaveMode.PRESERVE_OVERRIDES)
        assertEquals(Dependent(3, 2), loader.load(null, candidate.root))
        assertEquals(stringMappingOf("base" to i(1)), user)
    }

    @Test fun minimalRemovalStillRequiresFullValueEquality() {
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { it ?: i(2) }
        val candidate = planner.plan(i(1), i(2), i(1), YamlSaveMode.MINIMAL_DIFFERENCE)
        assertNull(candidate.root)
        assertNull(candidate.patches.fold(i(1) as com.github.inkm3.yamlconfig.node.YamlNode?) {
            node, patch -> patch.applyTo(node)
        })
    }

    @Test fun mismatchedCandidatesAreNotAccepted() {
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { i(99) }
        assertFailsWith<SerializationException> { planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES) }
    }

    @Test fun unexpectedValidatorFailuresAreNotHidden() {
        val problem = IllegalStateException("validator failed")
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { throw problem }
        val thrown = assertFailsWith<IllegalStateException> {
            planner.plan(i(1), i(2), i(1), YamlSaveMode.PRESERVE_OVERRIDES)
        }
        assertSame(problem, thrown)
    }

    @Test fun actualEditorValidationRetainsTheOriginalDecodingFailure() {
        val problem = SerializationException("invalid root")
        val planner = YamlSaveCandidatePlanner(serializer<Int>().descriptor) { throw problem }
        assertSame(problem, assertFailsWith<SerializationException> { planner.requireMatches(i(1), i(1)) })
    }
}
