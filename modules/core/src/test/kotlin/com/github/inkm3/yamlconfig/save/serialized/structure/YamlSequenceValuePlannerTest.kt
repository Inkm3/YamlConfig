package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSequenceStep
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralValuePlanner
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.apply
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.plan
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class YamlSequenceValuePlannerTest {
    @Test fun exactMoveKeepsTheWholeOriginalObjectNode() {
        val a = stringMappingOf("name" to s("a"), "extra" to s("a-data"))
        val b = stringMappingOf("name" to s("b"), "extra" to s("b-data"))
        val user = sequenceOf(a, b)
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(listOf(StructuralItem("a"), StructuralItem("b")),
            listOf(StructuralItem("b"), StructuralItem("a")), user))
        assertEquals(listOf(YamlSequenceStep.Move(1, 0)), edit.steps)
        apply(user, edit, sequenceOf(b, a))
    }

    @Test fun objectUpdateChangesOnlyKnownChildAndRetainsUnknown() {
        val user = sequenceOf(stringMappingOf("name" to s("a"), "extra" to s("keep")))
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(listOf(StructuralItem("a")),
            listOf(StructuralItem("a", 30000)), user))
        assertIs<YamlStructuralEdit.Mapping>(assertIs<YamlSequenceStep.Update>(edit.steps.single()).edit)
        apply(user, edit, sequenceOf(stringMappingOf("name" to s("a"), "port" to i(30000), "extra" to s("keep"))))
    }

    @Test fun scalarLexemeIsRetainedWhenAnotherElementIsInserted() {
        val raw = YamlScalarNode("+1", YamlScalarKind.INTEGER)
        val user = sequenceOf(raw, i(2))
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(listOf(1, 2), listOf(3, 1, 2), user))
        apply(user, edit, sequenceOf(i(3), raw, i(2)))
    }

    @Test fun explicitEmptyOverrideKeepsSequenceContainer() {
        val user = sequenceOf(i(1))
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(listOf(1), emptyList<Int>(), user))
        apply(user, edit, sequenceOf())
    }

    @Test fun inheritedSequenceUsesSetNotAnEditorForAMissingSequence() {
        val edit = YamlStructuralValuePlanner.plan(serializer<List<Int>>().descriptor,
            sequenceOf(i(1)), sequenceOf(i(2)), null)
        assertIs<YamlStructuralEdit.Set>(edit)
        apply(null, edit, sequenceOf(i(2)))
    }

    @Test fun sourceLengthMismatchFallsBackRatherThanUsingWrongIndices() {
        val user = sequenceOf(i(1), i(1))
        val edit = assertIs<YamlStructuralEdit.Set>(plan(listOf(1), listOf(2), user))
        apply(user, edit, sequenceOf(i(2)))
    }

    @Test fun sourceReorderingMismatchIsNotUsedForMetadataMatching() {
        val user = sequenceOf(stringMappingOf("name" to s("b")), stringMappingOf("name" to s("a")))
        val before = listOf(StructuralItem("a"), StructuralItem("b"))
        val after = listOf(StructuralItem("a", 30000), StructuralItem("b"))
        val edit = assertIs<YamlStructuralEdit.Set>(plan(before, after, user))
        apply(user, edit, YamlSerialization.Default.encodeToNode(after))
    }

    @Test fun duplicateEqualItemsKeepDeterministicOriginalOrder() {
        val first = stringMappingOf("name" to s("a"), "extra" to s("first"))
        val second = stringMappingOf("name" to s("a"), "extra" to s("second"))
        val user = sequenceOf(first, second)
        val edit = plan(listOf(StructuralItem("a"), StructuralItem("a")), listOf(StructuralItem("a")), user)
        apply(user, edit, sequenceOf(first))
    }

    @Test fun forceMaterializesDefaultsInsideOtherwiseUnchangedElements() {
        val user = sequenceOf(stringMappingOf("name" to s("a"), "extra" to s("keep")))
        val values = listOf(StructuralItem("a"))
        assertNull(plan(values, values, user))
        val edit = plan(values, values, user, force = true)
        apply(user, edit, sequenceOf(stringMappingOf("name" to s("a"), "port" to i(25565), "extra" to s("keep"))))
    }

    @Test fun nestedListsUseRecursiveSequenceEdits() {
        val raw = YamlScalarNode("+1", YamlScalarKind.INTEGER)
        val user = sequenceOf(sequenceOf(raw))
        val edit = plan(listOf(listOf(1)), listOf(listOf(1, 2)), user)
        apply(user, edit, sequenceOf(sequenceOf(raw, i(2))))
    }
}
