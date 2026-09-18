package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlSequenceDiffTest {
    @Test fun unchangedListProducesNoEdits() {
        assertTrue(YamlSequenceDiff.calculate(listOf("A", "B"), listOf("A", "B"), String::equals).isEmpty())
    }

    @Test fun frontInsertionPreservesExistingElements() {
        assertEquals(
            listOf(YamlSequenceEdit.Insert(0, "X")),
            YamlSequenceDiff.calculate(listOf("A", "B"), listOf("X", "A", "B"), String::equals),
        )
    }

    @Test fun removalIsEmittedFromTailAfterAlignment() {
        assertEquals(
            listOf(YamlSequenceEdit.Move<String>(2, 1), YamlSequenceEdit.Remove<String>(2)),
            YamlSequenceDiff.calculate(listOf("A", "B", "C"), listOf("A", "C"), String::equals),
        )
    }

    @Test fun reorderUsesMove() {
        assertEquals(
            listOf(YamlSequenceEdit.Move<String>(2, 0)),
            YamlSequenceDiff.calculate(listOf("A", "B", "C"), listOf("C", "A", "B"), String::equals),
        )
    }

    @Test fun inPlaceChangeUsesUpdate() {
        assertEquals(
            listOf(YamlSequenceEdit.Update(1, "B", "X")),
            YamlSequenceDiff.calculate(listOf("A", "B", "C"), listOf("A", "X", "C"), String::equals),
        )
    }

    @Test fun generatedEditsAlwaysTransformBaselineIntoCurrentForSmallInputs() {
        val alphabet = listOf("A", "B", "C")
        val lists = buildList {
            add(emptyList())
            for (length in 1..4) addAll(sequences(alphabet, length))
        }

        for (baseline in lists) {
            for (current in lists) {
                val working = baseline.toMutableList()
                for (edit in YamlSequenceDiff.calculate(baseline, current, String::equals)) {
                    when (edit) {
                        is YamlSequenceEdit.Insert -> working.add(edit.index, edit.value)
                        is YamlSequenceEdit.Update -> working[edit.index] = edit.current
                        is YamlSequenceEdit.Move -> {
                            val value = working.removeAt(edit.fromIndex)
                            working.add(edit.toIndex, value)
                        }
                        is YamlSequenceEdit.Remove -> working.removeAt(edit.index)
                    }
                }
                assertEquals(current, working, "baseline=$baseline current=$current")
            }
        }
    }

    private fun sequences(values: List<String>, length: Int): List<List<String>> {
        if (length == 0) return listOf(emptyList())
        return sequences(values, length - 1).flatMap { prefix -> values.map { prefix + it } }
    }
}
