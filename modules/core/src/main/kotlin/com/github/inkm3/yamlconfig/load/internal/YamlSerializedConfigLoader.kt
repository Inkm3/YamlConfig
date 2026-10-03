package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.YamlConfigSession
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSerializedSessionSaver
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.internal.YamlMissingFileHandler
import com.github.inkm3.yamlconfig.spi.YamlEngine
import kotlinx.serialization.KSerializer

internal class YamlSerializedConfigLoader<T : Any>(
    private val engine: YamlEngine,
    private val source: YamlSource,
    private val serializer: KSerializer<T>,
    private val serialization: YamlSerialization,
    private val defaults: YamlInput?,
    private val saveMode: YamlSaveMode,
    private val missingFilePolicy: YamlMissingFilePolicy,
) {
    internal fun load(): YamlConfigSession<T> {
        YamlMissingFileHandler.handle(source, defaults, missingFilePolicy)
        val defaultsRoot = defaults?.let(engine::parse)?.root
        val document = engine.parse(source)
        val value = YamlConfigValueLoader(serializer, serialization).load(defaultsRoot, document.root)
        val saver = YamlSerializedSessionSaver(value, serializer, serialization, defaultsRoot,
            document.editor(), engine, source, saveMode)
        return YamlConfigSession(value, saver)
    }
}
