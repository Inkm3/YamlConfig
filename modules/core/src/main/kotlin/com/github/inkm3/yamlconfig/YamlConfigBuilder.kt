package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.spi.YamlEngine
import kotlinx.serialization.KSerializer

/**
 * Java-friendly optional configuration over the same serializer load/save path.
 * [build] snapshots options, not the supplied sources, engine or serializer.
 * Mutating this builder afterwards does not reconfigure an already built config.
 */
public class YamlConfigBuilder<T : Any> internal constructor(
    private val engine: YamlEngine,
    private val userSource: YamlSource,
    private val serializer: KSerializer<T>,
) {
    private var defaults: YamlInput? = null
    private var mode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES
    private var missingPolicy: YamlMissingFilePolicy? = null
    private var format: YamlSerialization = YamlSerialization.Default

    /** A null input removes the defaults source. No input is opened here. */
    public fun defaultsSource(input: YamlInput?): YamlConfigBuilder<T> = apply {
        defaults = input
    }

    public fun saveMode(mode: YamlSaveMode): YamlConfigBuilder<T> = apply {
        this.mode = mode
    }

    /** Overrides the automatic policy. It stays explicit if defaults are changed. */
    public fun missingFilePolicy(policy: YamlMissingFilePolicy): YamlConfigBuilder<T> = apply {
        missingPolicy = policy
    }

    /** Restores COPY_DEFAULTS with defaults, otherwise DO_NOT_CREATE, at build time. */
    public fun automaticMissingFilePolicy(): YamlConfigBuilder<T> = apply {
        missingPolicy = null
    }

    public fun serialization(format: YamlSerialization): YamlConfigBuilder<T> = apply {
        this.format = format
    }

    /** Builds a reusable config without parsing, creating, or writing any source. */
    public fun build(): YamlConfig<T> = yamlConfig(
        engine = engine,
        userSource = userSource,
        serializer = serializer,
        defaultsSource = defaults,
        saveMode = mode,
        missingFilePolicy = missingPolicy ?: if (defaults != null) {
            YamlMissingFilePolicy.COPY_DEFAULTS
        } else {
            YamlMissingFilePolicy.DO_NOT_CREATE
        },
        serialization = format,
    )
}
