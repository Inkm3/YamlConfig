package com.github.inkm3.yamlconfig.schema.scalar

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlKeySchema
import com.github.inkm3.yamlconfig.schema.internal.YamlSchemaSupport

internal abstract class AbstractYamlScalarSchema<T>(
    private val kind: YamlScalarKind,
): YamlKeySchema<T>() {

    final override fun decode(node: YamlNode, path: YamlPath): T {
        if (node !is YamlScalarNode) {
            throw YamlSchemaException(
                path,
                "Expected ${scalarTypeName(kind)}, but found ${nodeTypeName(node)}",
            )
        }

        if (node.kind != kind) {
            throw YamlSchemaException(
                path,
                "Expected ${scalarTypeName(kind)}, but found ${scalarTypeName(node.kind)}"
            )
        }

        return decodeValue(node.value, path)
    }

    final override fun encode(value: T): YamlNode {
        return YamlScalarNode(encodeValue(value), kind)
    }

    final override fun decodeKey(key: YamlMapKey, path: YamlPath): T {
        if (key.kind != kind) {
            throw YamlSchemaException(
                path,
                "Expected ${scalarTypeName(kind)} mapping key, but found ${scalarTypeName(key.kind)}",
            )
        }

        return decodeValue(key.value, path)
    }

    final override fun encodeKey(value: T): YamlMapKey {
        return YamlMapKey(encodeValue(value), kind)
    }

    protected abstract fun decodeValue(value: String, path: YamlPath): T
    protected abstract fun encodeValue(value: T): String

    private fun nodeTypeName(node: YamlNode): String {
        return YamlSchemaSupport.nodeType(node)
    }

    private fun scalarTypeName(kind: YamlScalarKind): String {
        return YamlSchemaSupport.scalarType(kind)
    }
}