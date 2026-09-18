package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.scalar.internal.YamlScalarLexicalCodec

internal object YamlFloatSchema: AbstractYamlScalarSchema<Float>(YamlScalarKind.FLOAT) {

    override fun decodeValue(value: String, path: YamlPath): Float {
        return YamlScalarLexicalCodec.decodeFloat(value)
            ?: throw YamlSchemaException(path, "Invalid floating-point value: $value")
    }

    override fun encodeValue(value: Float): String {
        return YamlScalarLexicalCodec.encodeFloat(value)
    }

    override fun equivalent(first: Float, second: Float): Boolean {
        return first == second || (first.isNaN() && second.isNaN())
    }
}