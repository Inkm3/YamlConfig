package com.github.inkm3.yamlconfig.save.internal.strategy

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.schema.objectmapping.YamlObjectField

internal class YamlObjectSaveStrategy<T : Any>(
    private val factory: () -> T,
    private val fields: List<YamlObjectField<T>>,
): YamlSaveStrategy<T> {

    override fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
    ): Unit {
        val initial = factory()
        for (field in fields) {
            field.save(
                context,
                path,
                current,
                baseline,
                fallbackDefault,
                initial,
            )
        }
    }
}