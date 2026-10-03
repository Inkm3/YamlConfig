package com.github.inkm3.yamlconfig.exception

public open class YamlParseException(
    public val inputDescription: String,
    message: String,
    cause: Throwable? = null,
): YamlConfigException(message, cause)