package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.spi.YamlEngine

/**
 * Creates a YAML configuration.
 *
 * When [missingFilePolicy] is not specified:
 *
 * - [YamlMissingFilePolicy.COPY_DEFAULTS] is used when [defaultsSource]
 *   is configured.
 * - [YamlMissingFilePolicy.DO_NOT_CREATE] is used otherwise.
 */
public fun <T: Any> yamlConfig(
    engine: YamlEngine,
    userSource: YamlSource,
    schema: YamlSchema<T>,
    defaultsSource: YamlInput? = null,
    saveMode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES,
    missingFilePolicy: YamlMissingFilePolicy = (
        if (defaultsSource != null) {
            YamlMissingFilePolicy.COPY_DEFAULTS
        } else {
            YamlMissingFilePolicy.DO_NOT_CREATE
        }
    ),
): YamlConfig<T> {
    return YamlConfig(
        engine,
        userSource,
        schema,
        defaultsSource,
        saveMode,
        missingFilePolicy,
    )
}