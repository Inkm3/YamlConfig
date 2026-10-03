package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.load.internal.YamlSerializedConfigLoader
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.spi.YamlEngine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Creates a serializer-backed Config. Unknown string properties are tolerated on
 * load; save validates the full candidate against the same parsed defaults.
 * Sessions require external synchronization. See docs/transactional-save.md.
 */
public fun <T : Any> yamlConfig(
    engine: YamlEngine,
    userSource: YamlSource,
    serializer: KSerializer<T>,
    defaultsSource: YamlInput? = null,
    saveMode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES,
    missingFilePolicy: YamlMissingFilePolicy = if (defaultsSource != null)
        YamlMissingFilePolicy.COPY_DEFAULTS else YamlMissingFilePolicy.DO_NOT_CREATE,
    serialization: YamlSerialization = YamlSerialization.Default,
): YamlConfig<T> {
    val loader = YamlSerializedConfigLoader(engine, userSource, serializer, serialization,
        defaultsSource, saveMode, missingFilePolicy)
    return YamlConfig(loader::load)
}

/** Resolves [T] through [serialization]'s module, including contextual registration. */
public inline fun <reified T : Any> yamlConfig(
    engine: YamlEngine,
    userSource: YamlSource,
    defaultsSource: YamlInput? = null,
    saveMode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES,
    missingFilePolicy: YamlMissingFilePolicy = if (defaultsSource != null)
        YamlMissingFilePolicy.COPY_DEFAULTS else YamlMissingFilePolicy.DO_NOT_CREATE,
    serialization: YamlSerialization = YamlSerialization.Default,
): YamlConfig<T> = yamlConfig(engine, userSource, serialization.serializersModule.serializer<T>(),
    defaultsSource, saveMode, missingFilePolicy, serialization)
