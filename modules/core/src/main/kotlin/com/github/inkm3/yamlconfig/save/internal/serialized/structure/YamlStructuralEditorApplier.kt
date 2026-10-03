package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.spi.YamlEditor

/** Applies a verified edit to a working/forked editor. Transaction owns rollback. */
internal object YamlStructuralEditorApplier {
    internal fun apply(editor: YamlEditor, path: YamlPath, edit: YamlStructuralEdit) {
        when (edit) {
            is YamlStructuralEdit.Set -> {
                if (edit.value == null) editor.remove(path) else editor.set(path, edit.value)
            }
            is YamlStructuralEdit.Mapping -> {
                for ((key, change) in edit.changes) apply(editor, path.child(key), change)
            }
            is YamlStructuralEdit.Sequence -> sequence(editor, path, edit)
        }
    }

    private fun sequence(editor: YamlEditor, path: YamlPath, edit: YamlStructuralEdit.Sequence) {
        val sequence = editor.sequence(path)
        for (step in edit.steps) {
            when (step) {
                is YamlSequenceStep.Insert -> {
                    require(step.index in 0..sequence.size)
                    sequence.insert(step.index, step.value)
                }
                is YamlSequenceStep.Remove -> {
                    require(step.index in 0 until sequence.size)
                    sequence.remove(step.index)
                }
                is YamlSequenceStep.Move -> {
                    require(step.from in 0 until sequence.size && step.to in 0 until sequence.size)
                    sequence.move(step.from, step.to)
                }
                is YamlSequenceStep.Update -> {
                    require(step.index in 0 until sequence.size)
                    require(step.edit !is YamlStructuralEdit.Set || step.edit.value != null) {
                        "Use Remove, not a null Update, to delete a sequence element"
                    }
                    apply(editor, path.child(step.index), step.edit)
                }
            }
        }
    }
}
