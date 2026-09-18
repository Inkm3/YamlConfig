package com.github.inkm3.yamlconfig.node

public data class YamlScalarNode(
    val value: String,
    val kind: YamlScalarKind,
): YamlNode