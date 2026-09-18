package com.github.inkm3.yamlconfig.save.internal.strategy

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveDispatcher
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.schema.wrapper.YamlNullableSchema

internal class YamlNullableSaveStrategy<T>(
    private val schema: YamlNullableSchema<T>,
): YamlSaveStrategy<T?> {

    override fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: T?,
        baseline: T?,
        fallbackDefault: YamlSaveValue<T?>,
    ): Unit {
        if (current == null || baseline == null) {
            YamlAtomicSaveStrategy(schema).save(context, path, current, baseline, fallbackDefault)
            return
        }

        YamlSaveDispatcher.save(
            schema.inner,
            context,
            path,
            current,
            baseline,
            fallbackDefault.toInnerFallback(),
        )
    }

    private fun YamlSaveValue<T?>.toInnerFallback(): YamlSaveValue<T> {
        return when (this) {
            is YamlSaveValue.Present -> {
                val value = value

                if (value == null) {
                    YamlSaveValue.Missing
                } else {
                    YamlSaveValue.Present(value)
                }
            }

            YamlSaveValue.Missing -> YamlSaveValue.Missing
        }
    }
}