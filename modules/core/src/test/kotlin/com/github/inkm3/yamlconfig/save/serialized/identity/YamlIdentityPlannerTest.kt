package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSequenceStep
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralItem
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.apply
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.plan
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class YamlIdentityPlannerTest {
    private fun node(name: String, port: Int = 25565, extra: String = name): YamlNode =
        stringMappingOf("name" to s(name), "port" to i(port), "extra" to s(extra))

    @Test fun moveAndUpdateReusesTheCorrectRawNode() {
        val user = sequenceOf(node("a"), node("b", 2))
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(
            listOf(StructuralItem("a"), StructuralItem("b", 2)),
            listOf(StructuralItem("b", 3), StructuralItem("a")), user))
        assertEquals(YamlSequenceStep.Move(1, 0), edit.steps[0])
        assertEquals(0, assertIs<YamlSequenceStep.Update>(edit.steps[1]).index)
        assertEquals(2, edit.steps.size)
        apply(user, edit, sequenceOf(node("b", 3), node("a")))
    }

    @Test fun multipleChangedItemsCanBeReorderedTogether() {
        val user = sequenceOf(node("a", 1), node("b", 2), node("c", 3))
        val before = listOf(StructuralItem("a", 1), StructuralItem("b", 2), StructuralItem("c", 3))
        val after = listOf(StructuralItem("c", 30), StructuralItem("a", 10), StructuralItem("b", 20))
        apply(user, plan(before, after, user), sequenceOf(node("c", 30), node("a", 10), node("b", 20)))
    }

    @Test fun insertingNewItemDoesNotConsumeChangedItemNeededLater() {
        val user = sequenceOf(node("a", 1), node("b", 2))
        val after = listOf(StructuralItem("x", 9), StructuralItem("a", 10), StructuralItem("b", 2))
        val inserted = YamlSerialization.Default.encodeToNode(StructuralItem("x", 9))
        apply(user, plan(listOf(StructuralItem("a", 1), StructuralItem("b", 2)), after, user),
            sequenceOf(inserted, node("a", 10), node("b", 2)))
    }

    @Test fun identityRenameDoesNotTransferUnknownFields() {
        val user = sequenceOf(node("a", 1, "old-owner"))
        val after = listOf(StructuralItem("b", 1))
        val edit = assertIs<YamlStructuralEdit.Sequence>(plan(listOf(StructuralItem("a", 1)), after, user))
        assertIs<YamlSequenceStep.Remove>(edit.steps[0])
        assertIs<YamlSequenceStep.Insert>(edit.steps[1])
        apply(user, edit, YamlSerialization.Default.encodeToNode(after))
    }

    @Test fun duplicateIdentityChangedValuesAreReplacementsNotGuessedUpdates() {
        val user = sequenceOf(node("a", 1, "first"), node("a", 2, "second"))
        val after = listOf(StructuralItem("a", 3))
        apply(user, plan(listOf(StructuralItem("a", 1), StructuralItem("a", 2)), after, user),
            YamlSerialization.Default.encodeToNode(after))
    }

    @Test fun exactValueStillWinsInsideDuplicateIdentityGroup() {
        val user = sequenceOf(node("a", 1, "first"), node("a", 2, "second"))
        val after = listOf(StructuralItem("a", 2))
        apply(user, plan(listOf(StructuralItem("a", 1), StructuralItem("a", 2)), after, user),
            sequenceOf(node("a", 2, "second")))
    }

    @Test fun forceRepairTargetsIdentityMatchedAndMovedItems() {
        val user = sequenceOf(stringMappingOf("name" to s("a"), "extra" to s("a")),
            stringMappingOf("name" to s("b"), "extra" to s("b")))
        apply(user, plan(listOf(StructuralItem("a"), StructuralItem("b")),
            listOf(StructuralItem("b", 3), StructuralItem("a")), user, force = true),
            sequenceOf(node("b", 3), node("a")))
    }

    @Test fun mismatchedSourceStillUsesWholeReplacementDespiteMatchingIdentities() {
        val user = sequenceOf(node("b", 2), node("a", 1))
        val after = listOf(StructuralItem("b", 3), StructuralItem("a", 1))
        val edit = assertIs<YamlStructuralEdit.Set>(plan(
            listOf(StructuralItem("a", 1), StructuralItem("b", 2)), after, user))
        apply(user, edit, YamlSerialization.Default.encodeToNode(after))
    }
}
