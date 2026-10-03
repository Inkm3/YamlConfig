package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlSequenceIdentityDiffTest {
    private data class Item(val id: String, val value: Int)
    private fun diff(before: List<Item>, after: List<Item>): List<YamlSequenceEdit<Item>> {
        val oldCounts = before.groupingBy { it.id }.eachCount()
        val newCounts = after.groupingBy { it.id }.eachCount()
        val same = { a: Item, b: Item -> a.id == b.id && oldCounts[a.id] == 1 && newCounts[a.id] == 1 }
        return YamlSequenceDiff.calculate(before, after, { a, b -> a == b }, same, same)
    }
    private fun replay(before: List<Item>, edits: List<YamlSequenceEdit<Item>>): List<Item> {
        val working = before.toMutableList()
        for (edit in edits) when (edit) {
            is YamlSequenceEdit.Insert -> working.add(edit.index, edit.value)
            is YamlSequenceEdit.Remove -> working.removeAt(edit.index)
            is YamlSequenceEdit.Move -> working.add(edit.toIndex, working.removeAt(edit.fromIndex))
            is YamlSequenceEdit.Update -> {
                assertEquals(edit.baseline, working[edit.index])
                assertEquals(edit.baseline.id, edit.current.id)
                working[edit.index] = edit.current
            }
        }
        return working
    }
    @Test fun changedMovedItemUsesMoveThenUpdate() {
        val a = Item("a", 1); val b = Item("b", 2); val updated = b.copy(value = 3)
        assertEquals(listOf(YamlSequenceEdit.Move<Item>(1, 0), YamlSequenceEdit.Update(0, b, updated)),
            diff(listOf(a, b), listOf(updated, a)))
    }
    @Test fun exactMatchMovesBeforeIdentityBasedUpdate() {
        val a = Item("a", 1); val b = Item("b", 2); val updated = a.copy(value = 3)
        assertEquals(listOf(YamlSequenceEdit.Move<Item>(1, 0), YamlSequenceEdit.Update(1, a, updated)),
            diff(listOf(a, b), listOf(b, updated)))
    }
    @Test fun unchangedPositionOnlyUpdatesTheSameIdentity() {
        val before = Item("a", 1); val after = before.copy(value = 2)
        assertEquals(listOf(YamlSequenceEdit.Update(0, before, after)), diff(listOf(before), listOf(after)))
    }
    @Test fun changedIdentityIsRemovedAndInsertedNotUpdated() {
        val before = Item("a", 1); val after = Item("b", 2)
        assertEquals(listOf(YamlSequenceEdit.Remove<Item>(0), YamlSequenceEdit.Insert(0, after)),
            diff(listOf(before), listOf(after)))
    }
    @Test fun changedExistingItemNeededLaterIsNotOverwrittenByANewItem() {
        val a = Item("a", 1); val b = Item("b", 1); val x = Item("x", 1); val changed = a.copy(value = 2)
        assertEquals(listOf(YamlSequenceEdit.Insert(0, x), YamlSequenceEdit.Update(1, a, changed)),
            diff(listOf(a, b), listOf(x, changed, b)))
    }
    @Test fun duplicateIdentityDoesNotBecomeUniqueAfterOneDeletion() {
        val before = listOf(Item("a", 1), Item("a", 2)); val after = listOf(Item("a", 3))
        val edits = diff(before, after)
        assertTrue(edits.none { it is YamlSequenceEdit.Update })
        assertEquals(after, replay(before, edits))
    }
    @Test fun legacyOverloadStillAllowsPositionalUpdatesAndTrailingLambda() {
        val before = Item("a", 1); val after = Item("b", 2)
        assertEquals(listOf(YamlSequenceEdit.Update(0, before, after)),
            YamlSequenceDiff.calculate(listOf(before), listOf(after)) { a, b -> a == b })
    }
    @Test fun allSmallIdentityListsReplayWithoutCrossIdentityUpdates() {
        val alphabet = listOf(Item("a", 0), Item("a", 1), Item("b", 0), Item("b", 1))
        val lists = mutableListOf<List<Item>>(emptyList())
        var level: List<List<Item>> = listOf(emptyList())
        repeat(3) { level = level.flatMap { prefix -> alphabet.map { prefix + it } }; lists.addAll(level) }
        var pairs = 0
        for (before in lists) for (after in lists) {
            assertEquals(after, replay(before, diff(before, after)))
            pairs++
        }
        assertEquals(7225, pairs)
    }
}
