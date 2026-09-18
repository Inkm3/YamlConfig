package com.github.inkm3.yamlconfig.node

public data class YamlMapKey(
    public val value: String,
    public val kind: YamlScalarKind = YamlScalarKind.STRING,
)
