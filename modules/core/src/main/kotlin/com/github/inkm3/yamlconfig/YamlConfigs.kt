package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.spi.YamlEngine

public fun <T> yamlConfig(
    engine: YamlEngine,
    userSource: YamlSource,
    schema: YamlSchema<T>,
    defaultsSource: YamlInput? = null,
    saveMode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES,
): YamlConfig<T> {
    return YamlConfig(
        engine,
        userSource,
        schema,
        defaultsSource,
        saveMode,
    )
}