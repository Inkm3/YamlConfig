package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.save.internal.diff.YamlIndexedSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.identity.YamlSequenceIdentityMatcher
import kotlinx.serialization.descriptors.SerialDescriptor

/** Exact moves first, then unique identities. Updates recurse into known fields. */
internal object YamlSequenceValuePlanner {
    internal fun plan(
        element: SerialDescriptor,
        baseline: YamlSequenceNode,
        current: YamlSequenceNode,
        user: YamlSequenceNode,
        force: Boolean,
        retainOmitted: Boolean = false,
        alignment: YamlSourceAlignmentMemo = YamlSourceAlignmentMemo(),
    ): YamlStructuralEdit? {
        val working = user.elements.toMutableList()
        val steps = mutableListOf<YamlSequenceStep>()
        val identity = YamlSequenceIdentityMatcher.create(element, baseline.elements, current.elements)
        val edits = if (minOf(baseline.size, current.size) >= 128) {
            val key: (YamlNode) -> Any? = if (identity == null) { _ -> null } else identity::uniqueKey
            val update: (YamlNode, YamlNode) -> Boolean = if (identity == null) { _, _ -> true } else identity::matches
            YamlIndexedSequenceDiff.calculate(baseline.elements, current.elements,
                { a, b -> a == b }, { it.hashCode() }, key, update)
        } else if (identity == null) {
            YamlSequenceDiff.calculate(baseline.elements, current.elements) { a, b -> a == b }
        } else {
            YamlSequenceDiff.calculate(baseline.elements, current.elements, { a, b -> a == b },
                identity::matches, identity::matches)
        }
        for (edit in edits) {
            when (edit) {
                is YamlSequenceEdit.Insert -> {
                    working.add(edit.index, edit.value)
                    steps += YamlSequenceStep.Insert(edit.index, edit.value)
                }
                is YamlSequenceEdit.Remove -> {
                    working.removeAt(edit.index)
                    steps += YamlSequenceStep.Remove(edit.index)
                }
                is YamlSequenceEdit.Move -> {
                    working.add(edit.toIndex, working.removeAt(edit.fromIndex))
                    steps += YamlSequenceStep.Move(edit.fromIndex, edit.toIndex)
                }
                is YamlSequenceEdit.Update -> update(element, edit.index, edit.baseline, edit.current,
                    working, steps, force, retainOmitted, alignment)
            }
        }
        if (force) {
            for (index in current.elements.indices) update(element, index, current[index], current[index],
                working, steps, true, retainOmitted, alignment)
        }
        return steps.takeIf { it.isNotEmpty() }?.let(YamlStructuralEdit::Sequence)
    }

    private fun update(
        descriptor: SerialDescriptor, index: Int, baseline: YamlNode, current: YamlNode,
        working: MutableList<YamlNode>, steps: MutableList<YamlSequenceStep>, force: Boolean,
        retainOmitted: Boolean, alignment: YamlSourceAlignmentMemo,
    ) {
        val edit = YamlStructuralValuePlanner.plan(descriptor, baseline, current, working[index], force,
            retainOmitted = retainOmitted, alignment = alignment) ?: return
        working[index] = requireNotNull(YamlStructuralNodeEditor.apply(working[index], edit))
        steps += YamlSequenceStep.Update(index, edit)
    }
}
