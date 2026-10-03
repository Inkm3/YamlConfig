package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlValuePatch
import com.github.inkm3.yamlconfig.save.internal.serialized.reduction.YamlRemovalCandidate
import com.github.inkm3.yamlconfig.testsupport.*
import kotlin.test.*

class YamlRemovalCandidateTest {
    private fun keys(vararg names: String) = names.map(::YamlMapKey)

    @Test fun missingPathReturnsOriginalRootWithoutEmptyParentCreation() {
        val initial = stringMappingOf("a" to i(1))
        assertSame(initial, YamlRemovalCandidate.remove(initial, keys("missing")))
        assertSame(initial, YamlRemovalCandidate.remove(initial, keys("missing", "child")))
        assertNull(YamlRemovalCandidate.remove(null, keys("missing", "child")))
    }

    @Test fun deepMissingPathReusesAllAncestors() {
        val initial = stringMappingOf("a" to stringMappingOf("b" to i(1)))
        assertSame(initial, YamlRemovalCandidate.remove(initial, keys("a", "absent", "child")))
    }

    @Test fun deletionOnlyCopiesAncestorsAndKeepsUntouchedChildReferences() {
        val sibling = sequenceOf(i(2), n())
        val child = stringMappingOf("remove" to i(1), "keep" to sibling)
        val initial = stringMappingOf("a" to child, "untouched" to sibling)
        val result = assertIs<YamlMappingNode>(YamlRemovalCandidate.remove(initial, keys("a", "remove")))
        assertNotSame(initial, result)
        val changed = assertIs<YamlMappingNode>(result["a"])
        assertNotSame(child, changed)
        assertSame(sibling, result["untouched"])
        assertSame(sibling, changed["keep"])
        assertEquals(i(1), child["remove"])
    }

    @Test fun removingLastChildKeepsPresentEmptyParent() {
        val initial = stringMappingOf("a" to stringMappingOf("b" to i(1)))
        assertEquals(stringMappingOf("a" to stringMappingOf()), YamlRemovalCandidate.remove(initial, keys("a", "b")))
    }

    @Test fun rootDeletionAndExplicitNullAreDistinctFromMissingPath() {
        assertNull(YamlRemovalCandidate.remove(n(), emptyList()))
        assertNull(YamlRemovalCandidate.remove(null, emptyList()))
        assertEquals(stringMappingOf(), YamlRemovalCandidate.remove(stringMappingOf("a" to n()), keys("a")))
    }

    @Test fun invalidContainerStillRaisesAnErrorInsteadOfRepairingIt() {
        for (node in listOf(i(1), n(), sequenceOf())) {
            assertFailsWith<IllegalArgumentException> { YamlRemovalCandidate.remove(node, keys("a")) }
        }
    }

    @Test fun rawKeyKindsAndSurvivingEntryOrderArePreserved() {
        val numeric = YamlMapKey("1", YamlScalarKind.INTEGER)
        val text = YamlMapKey("1")
        val initial = YamlMappingNode(linkedMapOf(numeric to i(10), text to i(20), YamlMapKey("z") to i(30)))
        val result = assertIs<YamlMappingNode>(YamlRemovalCandidate.remove(initial, listOf(text)))
        assertEquals(listOf(numeric, YamlMapKey("z")), result.entries.keys.toList())
        assertEquals(i(10), result[numeric])
    }

    @Test fun alreadyDeletedPathDoesNotProduceAnotherCandidate() {
        val initial = stringMappingOf("a" to i(1), "keep" to i(2))
        val once = YamlRemovalCandidate.remove(initial, keys("a"))
        assertSame(once, YamlRemovalCandidate.remove(once, keys("a")))
    }

    @Test fun specializedRemovalAgreesWithGeneralPatchForExistingAndMissingPaths() {
        val initial = stringMappingOf("a" to stringMappingOf("b" to i(1)), "c" to sequenceOf(i(2)))
        for (path in listOf(emptyList(), keys("a"), keys("a", "b"), keys("a", "missing"), keys("absent", "x"), keys("c"))) {
            assertEquals(YamlValuePatch(path, null).applyTo(initial).toString(),
                YamlRemovalCandidate.remove(initial, path).toString())
        }
    }
}
