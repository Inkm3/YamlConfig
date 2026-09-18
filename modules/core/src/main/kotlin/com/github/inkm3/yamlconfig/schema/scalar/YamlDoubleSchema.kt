package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.scalar.internal.YamlScalarLexicalCodec

internal object YamlDoubleSchema: AbstractYamlScalarSchema<Double>(YamlScalarKind.FLOAT) {

    override fun decodeValue(value: String, path: YamlPath): Double {
        return YamlScalarLexicalCodec.decodeDouble(value)
            ?: throw YamlSchemaException(path, "Invalid floating-point value: $value")
    }

    override fun encodeValue(value: Double): String {
        return YamlScalarLexicalCodec.encodeDouble(value)
    }

    override fun equivalent(first: Double, second: Double): Boolean {
        return first == second || (first.isNaN() && second.isNaN())
    }
}