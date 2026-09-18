package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.scalar.internal.YamlScalarLexicalCodec

internal object YamlIntSchema: AbstractYamlScalarSchema<Int>(YamlScalarKind.INTEGER) {

    override fun decodeValue(value: String, path: YamlPath): Int {
        return YamlScalarLexicalCodec.decodeInt(value)
            ?: throw YamlSchemaException(path, "Invalid integer value: $value")
    }

    override fun encodeValue(value: Int): String {
        return YamlScalarLexicalCodec.encodeInt(value)
    }

}