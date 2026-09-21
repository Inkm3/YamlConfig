package com.github.inkm3.yamlconfig.source

public interface YamlSource: YamlInput, YamlOutput {

    /**
     * Returns whether this source currently exists.
     *
     * A source that exists but contains no YAML content must return `true`.
     *
     * Returning `false` allows the configured [YamlMissingFilePolicy]
     * to be applied before the source is loaded.
     *
     * The default implementation returns `true`, so custom sources are
     * treated as existing unless they explicitly support missing-source
     * detection.
     */
    public fun exists(): Boolean = true
}