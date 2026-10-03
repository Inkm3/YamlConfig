package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.save.internal.diff.YamlIndexedSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlIndexedSequenceDiffTest {
    private data class Item(val id: String?, val value: Int)

    private fun <T> lists(alphabet: List<T>, depth: Int): List<List<T>> {
        val result = mutableListOf<List<T>>(emptyList())
        var level: List<List<T>> = listOf(emptyList())
        repeat(depth) {
            level = level.flatMap { prefix -> alphabet.map { prefix + it } }
            result.addAll(level)
        }
        return result
    }

    private fun <T> replay(before: List<T>, edits: List<YamlSequenceEdit<T>>): List<T> {
        val working = before.toMutableList()
        for (edit in edits) when (edit) {
            is YamlSequenceEdit.Insert -> working.add(edit.index, edit.value)
            is YamlSequenceEdit.Remove -> working.removeAt(edit.index)
            is YamlSequenceEdit.Move -> working.add(edit.toIndex, working.removeAt(edit.fromIndex))
            is YamlSequenceEdit.Update -> {
                assertEquals(edit.baseline, working[edit.index])
                working[edit.index] = edit.current
            }
        }
        return working
    }

    @Test fun allSmallValueListsPreserveExactEditOrderEvenWithHashCollisions() {
        val values = lists(listOf<Int?>(null, 1, 2), 4)
        var count = 0
        for (before in values) for (after in values) {
            val reference = YamlSequenceDiff.calculate(before, after) { a, b -> a == b }
            for (collision in listOf(false, true)) {
                val actual = YamlIndexedSequenceDiff.calculate(before, after, { a, b -> a == b },
                    { if (collision) 0 else it.hashCode() }, { null }, { _, _ -> true })
                assertEquals(reference, actual)
                assertEquals(after, replay(before, actual))
            }
            count++
        }
        assertEquals(14641, count)
    }

    private fun compareIdentity(before: List<Item>, after: List<Item>, collide: Boolean) {
        val oldCounts = before.groupingBy { it.id }.eachCount()
        val newCounts = after.groupingBy { it.id }.eachCount()
        val identity = { item: Item -> item.id?.takeIf { oldCounts[it] == 1 && newCounts[it] == 1 } }
        val same = { a: Item, b: Item -> identity(a)?.let { it == identity(b) } ?: false }
        val reference = YamlSequenceDiff.calculate(before, after, { a, b -> a == b }, same, same)
        val actual = YamlIndexedSequenceDiff.calculate(before, after, { a, b -> a == b },
            { if (collide) 0 else it.hashCode() }, identity, same)
        assertEquals(reference, actual)
        assertEquals(after, replay(before, actual))
    }

    @Test fun allSmallIdentityListsPreserveAmbiguityAndEditOrder() {
        val values = lists(listOf(Item("a", 0), Item("a", 1), Item("b", 0), Item(null, 1)), 3)
        var count = 0
        for (before in values) for (after in values) {
            compareIdentity(before, after, false)
            compareIdentity(before, after, true)
            count++
        }
        assertEquals(7225, count)
    }

    @Test fun largeMixedEditsMatchTheReferenceWithoutMutatingInput() {
        val random = Random(71351)
        repeat(60) {
            val before = List(160) { Item("item-$it", it) }
            val original = before.toList()
            val after = before.shuffled(random).filterIndexed { index, _ -> index % 7 != 0 }
                .mapIndexed { index, item -> if (index % 3 == 0) item.copy(value = -1) else item } + Item("new", 0)
            compareIdentity(before, after, false)
            assertEquals(original, before)
        }
    }

    @Test fun exactSearchUsesFewerExpensiveComparisonsForUniqueReverseOrder() {
        val before = (0 until 512).toList()
        val after = before.reversed()
        var referenceCalls = 0
        var indexedCalls = 0
        val old = YamlSequenceDiff.calculate(before, after) { a, b -> referenceCalls++; a == b }
        val new = YamlIndexedSequenceDiff.calculate(before, after,
            { a, b -> indexedCalls++; a == b }, { it }, { null }, { _, _ -> true })
        assertEquals(old, new)
        assertTrue(indexedCalls < referenceCalls / 20, "$indexedCalls versus $referenceCalls")
    }

    @Test fun repeatedReferenceDoesNotAcquireIdentityAfterDeletion() {
        val a = Item("a", 0)
        compareIdentity(listOf(a, a, Item("b", 0)), listOf(Item("a", 2), Item("b", 1)), false)
    }

    @Test fun unchangedAppendedAndTrimmedListsMatchReference() {
        val before = (0 until 150).toList()
        for (after in listOf(before, before + 151, before.take(75), emptyList())) {
            val expected = YamlSequenceDiff.calculate(before, after) { a, b -> a == b }
            val actual = YamlIndexedSequenceDiff.calculate(before, after, { a, b -> a == b },
                { it }, { null }, { _, _ -> true })
            assertEquals(expected, actual)
            assertEquals(after, replay(before, actual))
        }
    }
}
