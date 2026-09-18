package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.scalar.internal.YamlScalarLexicalCodec

internal object YamlLongSchema: AbstractYamlScalarSchema<Long>(YamlScalarKind.INTEGER) {

    override fun decodeValue(value: String, path: YamlPath): Long {
        return YamlScalarLexicalCodec.decodeLong(value)
            ?: throw YamlSchemaException(path, "Invalid long integer value: $value")
    }

    override fun encodeValue(value: Long): String {
        return YamlScalarLexicalCodec.encodeLong(value)
    }

}