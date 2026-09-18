package com.github.inkm3.yamlconfig.save.internal.action

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.schema.YamlSchema

internal object YamlSaveActionResolver {

    internal fun <T> resolve(schema: YamlSchema<T>, context: YamlSaveContext, path: YamlPath, current: T, baseline: T, fallbackDefault
    : YamlSaveValue<T>): YamlWriteAction {
        return when (context.mode) {
            YamlSaveMode.PRESERVE_OVERRIDES -> preserveOverrides(
                schema,
                current,
                baseline,
            )

            YamlSaveMode.MINIMAL_DIFFERENCE -> minimalDifference(
                schema,
                context,
                path,
                current,
                baseline,
                fallbackDefault,
            )
        }
    }

    private fun <T> preserveOverrides(
        schema: YamlSchema<T>,
        current: T,
        baseline: T,
    ): YamlWriteAction {
        return if (schema.equivalent(current, baseline)) {
            YamlWriteAction.KEEP
        } else {
            YamlWriteAction.SET
        }
    }

    private fun <T> minimalDifference(
        schema: YamlSchema<T>,
        context: YamlSaveContext,
        path: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
    ): YamlWriteAction {
        val defaultValue = context.resolveComparableDefault(
            schema,
            path,
            fallbackDefault,
        )

        if (defaultValue is YamlSaveValue.Present
            && schema.equivalent(current, defaultValue.value)) {
            return YamlWriteAction.REMOVE
        } else if (schema.equivalent(current, baseline)) {
            return YamlWriteAction.KEEP
        } else {
            return YamlWriteAction.SET
        }


    }
}