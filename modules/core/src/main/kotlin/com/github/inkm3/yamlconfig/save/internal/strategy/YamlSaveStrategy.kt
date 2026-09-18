package com.github.inkm3.yamlconfig.save.internal.strategy

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue

internal fun interface YamlSaveStrategy<T> {
    fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
    ): Unit
}