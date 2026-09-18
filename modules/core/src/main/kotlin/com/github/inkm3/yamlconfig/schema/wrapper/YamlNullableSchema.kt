package com.github.inkm3.yamlconfig.schema.wrapper

import com.github.inkm3.yamlconfig.context.YamlReadContext
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlSchema

internal class YamlNullableSchema<T>(
    internal val inner: YamlSchema<T>,
): YamlSchema<T?>() {

    override fun decode(node: YamlNode, path: YamlPath): T? {
        if (node is YamlScalarNode && node.kind == YamlScalarKind.NULL) {
            return null
        }

        return inner.decode(node, path)
    }

    override fun encode(value: T?): YamlNode {
        if (value == null) {
            return YamlScalarNode("null", YamlScalarKind.NULL)
        }

        return inner.encode(value)
    }

    override fun read(context: YamlReadContext, path: YamlPath): T? {
        val result = context.lookup(path)
        if (result is YamlNodeLookupResult.Found && result.node.isNull()) {
            return null
        }

        return inner.read(context, path)
    }

    override fun equivalent(first: T?, second: T?): Boolean {
        if (first == null || second == null) {
            return first == second
        }

        return inner.equivalent(first, second)
    }

    private fun YamlNode.isNull(): Boolean {
        return this is YamlScalarNode && kind == YamlScalarKind.NULL
    }
}