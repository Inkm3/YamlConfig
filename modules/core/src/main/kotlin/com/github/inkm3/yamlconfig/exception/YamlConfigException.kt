package com.github.inkm3.yamlconfig.exception

public open class YamlConfigException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)