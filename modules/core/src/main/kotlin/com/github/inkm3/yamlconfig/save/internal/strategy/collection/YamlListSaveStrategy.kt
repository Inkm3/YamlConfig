package com.github.inkm3.yamlconfig.save.internal.strategy.collection

import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveDispatcher
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.action.YamlSaveActionResolver
import com.github.inkm3.yamlconfig.save.internal.action.YamlWriteAction
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceDiff
import com.github.inkm3.yamlconfig.save.internal.diff.YamlSequenceEdit
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlSaveStrategy
import com.github.inkm3.yamlconfig.schema.collection.YamlListSchema

internal class YamlListSaveStrategy<T>(
    private val schema: YamlListSchema<T>,
): YamlSaveStrategy<List<T>> {
    override fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: List<T>,
        baseline: List<T>,
        fallbackDefault: YamlSaveValue<List<T>>
    ) {
        when (YamlSaveActionResolver.resolve(
            schema,
            context,
            path,
            current,
            baseline,
            fallbackDefault,
        )) {
            YamlWriteAction.KEEP -> return
            YamlWriteAction.SET -> Unit
            YamlWriteAction.REMOVE -> {
                context.writer.remove(path)
                return
            }
        }

        val userSequence = when (val result = context.lookupUser(path)) {
            is YamlNodeLookupResult.Found ->
                result.node as? YamlSequenceNode
            else -> null
        }

        if (userSequence == null) {
            context.writer.set(path, schema.encode(current))
            return
        }

        saveStructurally(
            context,
            path,
            current,
            baseline,
        )
    }

    private fun saveStructurally(
        context: YamlSaveContext,
        path: YamlPath,
        current: List<T>,
        baseline: List<T>,
    ): Unit {
        val childContext = context.withoutYamlDefaults()
        val sequence = context.writer.sequence(path)
        val edits = YamlSequenceDiff.calculate(
            baseline,
            current,
            schema.elementSchema::equivalent,
        )

        for (edit in edits) {
            when (edit) {
                is YamlSequenceEdit.Insert -> {
                    sequence.insert(
                        edit.index,
                        schema.elementSchema.encode(edit.value)
                    )
                }

                is YamlSequenceEdit.Move -> {
                    sequence.move(edit.fromIndex, edit.toIndex)
                }

                is YamlSequenceEdit.Update -> {
                    YamlSaveDispatcher.save(
                        schema.elementSchema,
                        childContext,
                        path.child(edit.index),
                        edit.current,
                        edit.baseline,
                        YamlSaveValue.Missing
                    )
                }

                is YamlSequenceEdit.Remove -> {
                    sequence.remove(edit.index)
                }
            }
        }
    }

}