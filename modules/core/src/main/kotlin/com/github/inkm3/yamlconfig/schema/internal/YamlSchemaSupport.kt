package com.github.inkm3.yamlconfig.schema.internal

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult

internal object YamlSchemaSupport {

    internal fun requireFound(
        result: YamlNodeLookupResult,
        expected: String,
    ): YamlNode {
        return when (result) {
            is YamlNodeLookupResult.Found -> result.node
            is YamlNodeLookupResult.Missing -> throw YamlSchemaException(
                result.path,
                "Missing $expected",
            )
            is YamlNodeLookupResult.ExpectedMapping -> throw YamlSchemaException(
                result.path,
                "Expected mapping, but found ${nodeType(result.actual)}",
            )
            is YamlNodeLookupResult.ExpectedSequence -> throw YamlSchemaException(
                result.path,
                "Expected sequence, but found ${nodeType(result.actual)}",
            )
            is YamlNodeLookupResult.IndexOutOfBounds -> throw YamlSchemaException(
                result.path,
                "Index ${result.index} is out of bounds (size: ${result.size})",
            )
        }
    }

    internal fun nodeType(node: YamlNode): String {
        return when (node) {
            is YamlMappingNode -> "mapping"
            is YamlSequenceNode -> "sequence"
            is YamlScalarNode -> scalarType(node.kind)
        }
    }

    internal fun scalarType(scalar: YamlScalarKind): String {
        return when (scalar) {
            YamlScalarKind.STRING -> "string"
            YamlScalarKind.BOOLEAN -> "boolean"
            YamlScalarKind.INTEGER -> "integer"
            YamlScalarKind.FLOAT -> "float"
            YamlScalarKind.NULL -> "null"
        }
    }
}