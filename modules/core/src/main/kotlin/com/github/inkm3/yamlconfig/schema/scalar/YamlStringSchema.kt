package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath

internal object YamlStringSchema: AbstractYamlScalarSchema<String>(YamlScalarKind.STRING) {

    override fun decodeValue(value: String, path: YamlPath): String {
        return value
    }

    override fun encodeValue(value: String): String {
        return value
    }

}