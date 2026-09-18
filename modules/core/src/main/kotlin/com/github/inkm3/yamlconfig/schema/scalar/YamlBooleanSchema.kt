package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.scalar.internal.YamlScalarLexicalCodec

internal object YamlBooleanSchema: AbstractYamlScalarSchema<Boolean>(YamlScalarKind.BOOLEAN) {

    override fun decodeValue(value: String, path: YamlPath): Boolean {
        return YamlScalarLexicalCodec.decodeBoolean(value)
            ?: throw YamlSchemaException(path, "Invalid boolean value: $value")
    }

    override fun encodeValue(value: Boolean): String {
        return YamlScalarLexicalCodec.encodeBoolean(value)
    }

}