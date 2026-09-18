package com.github.inkm3.yamlconfig.schema.objectmapping

import com.github.inkm3.yamlconfig.context.YamlReadContext
import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.schema.internal.YamlSchemaSupport

internal class YamlObjectSchema<T : Any>(
    internal val factory: () -> T,
    internal val fields: List<YamlObjectField<T>>,
): YamlSchema<T>() {

    override fun read(context: YamlReadContext, path: YamlPath): T {
        when (val result = context.lookup(path)) {
            is YamlNodeLookupResult.Found -> {
                if (result.node !is YamlMappingNode) {
                    throw YamlSchemaException(
                        path,
                        "Expected mapping, but found ${YamlSchemaSupport.nodeType(result.node)}",
                    )
                }
            }

            is YamlNodeLookupResult.Missing -> {

            }

            else -> {
                YamlSchemaSupport.requireFound(result, "mapping")
            }
        }

        val target = factory()
        for (field in fields) {
            field.read(context, path, target)
        }

        return target
    }

    override fun decode(node: YamlNode, path: YamlPath): T {
        if (node !is YamlMappingNode) {
            throw YamlSchemaException(
                path,
                "Expected mapping, but found ${YamlSchemaSupport.nodeType(node)}",
            )
        }

        val target = factory()
        for (field in fields) {
            field.decode(node, path, target)
        }

        return target
    }

    override fun encode(value: T): YamlNode {
        val initial = factory()

        val entries = linkedMapOf<YamlMapKey, YamlNode>()
        for (field in fields) {
            val entry = field.encode(value, initial)
                ?: continue
            entries[entry.first] = entry.second
        }

        return YamlMappingNode(entries)
    }

    override fun equivalent(first: T, second: T): Boolean {
        return fields.all { field -> field.equivalent(first, second) }
    }
}