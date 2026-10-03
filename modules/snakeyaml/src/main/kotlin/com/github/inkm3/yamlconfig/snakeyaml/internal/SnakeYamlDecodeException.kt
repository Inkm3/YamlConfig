package com.github.inkm3.yamlconfig.snakeyaml.internal

internal class SnakeYamlDecodeException(
    message: String,
    cause: Throwable? = null
): RuntimeException(message, cause)