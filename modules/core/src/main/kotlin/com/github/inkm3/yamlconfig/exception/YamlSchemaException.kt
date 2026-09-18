package com.github.inkm3.yamlconfig.exception

import com.github.inkm3.yamlconfig.path.YamlPath

public open class YamlSchemaException(
    public val path: YamlPath,
    message: String,
    cause: Throwable? = null,
): YamlConfigException("$message at $path", cause)