package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSequenceStep
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlStructuralSequenceReplayTest {
    @Test fun everySmallListDiffReplaysToTheTargetInBothAppliers() {
        val lists = mutableListOf<List<YamlNode>>(emptyList())
        var level: List<List<YamlNode>> = listOf(emptyList())
        repeat(4) {
            level = level.flatMap { prefix -> (0..2).map { prefix + i(it) } }
            lists.addAll(level)
        }
        var count = 0
        for (before in lists) for (after in lists) {
            val steps = YamlSequenceDiff.calculate(before, after) { a, b -> a == b }.map { step ->
                when (step) {
                    is YamlSequenceEdit.Insert -> YamlSequenceStep.Insert(step.index, step.value)
                    is YamlSequenceEdit.Remove -> YamlSequenceStep.Remove(step.index)
                    is YamlSequenceEdit.Move -> YamlSequenceStep.Move(step.fromIndex, step.toIndex)
                    is YamlSequenceEdit.Update -> YamlSequenceStep.Update(step.index, YamlStructuralEdit.Set(step.current))
                }
            }
            val edit = YamlStructuralEdit.Sequence(steps)
            val initial = YamlSequenceNode(before)
            val expected = YamlSequenceNode(after)
            val editor = TestYamlEditor(initial)
            assertEquals(expected, YamlStructuralNodeEditor.apply(initial, edit))
            YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
            assertEquals(expected, editor.root)
            assertEquals(YamlSequenceNode(before), initial)
            count++
        }
        assertEquals(14641, count)
    }
}
