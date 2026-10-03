package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.spi.YamlEngine
import kotlinx.serialization.KSerializer

/** Java entry points; the existing Kotlin top-level factories remain unchanged. */
public object YamlConfigs {
    /** Creates a config without performing I/O. Call [YamlConfig.load] to read it. */
    @JvmStatic
    public fun <T : Any> create(
        engine: YamlEngine,
        userSource: YamlSource,
        serializer: KSerializer<T>,
    ): YamlConfig<T> = yamlConfig(engine, userSource, serializer)

    /**
     * Configures optional arguments without Java having to supply Kotlin defaults.
     * The required dependencies are fixed; a builder is not thread-safe.
     */
    @JvmStatic
    public fun <T : Any> builder(
        engine: YamlEngine,
        userSource: YamlSource,
        serializer: KSerializer<T>,
    ): YamlConfigBuilder<T> = YamlConfigBuilder(engine, userSource, serializer)
}
