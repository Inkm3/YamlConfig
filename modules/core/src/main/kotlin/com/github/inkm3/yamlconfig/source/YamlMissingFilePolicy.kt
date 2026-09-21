package com.github.inkm3.yamlconfig.source

/**
 * Defines how a missing user YAML source is handled when loading a
 * configuration.
 *
 * This policy is applied only when [YamlSource.exists] returns `false`.
 * An existing but empty source is not considered missing.
 */
public enum class YamlMissingFilePolicy {

    /**
     * Leaves the missing source untouched.
     *
     * Loading continues without creating the user source.
     */
    DO_NOT_CREATE,

    /**
     * Creates an empty user source before loading it.
     */
    CREATE_EMPTY,

    /**
     * Creates the user source by copying the configured defaults verbatim.
     *
     * The original YAML text is copied without parsing and re-serializing it,
     * preserving comments and presentation.
     *
     * A defaults source must be configured when this policy is actually
     * applied.
     */
    COPY_DEFAULTS,
}