package com.github.inkm3.yamlconfig.exception

public open class YamlWriteException(
    public val outputDescription: String,
    message: String,
    cause: Throwable? = null,
): YamlConfigException(message, cause)