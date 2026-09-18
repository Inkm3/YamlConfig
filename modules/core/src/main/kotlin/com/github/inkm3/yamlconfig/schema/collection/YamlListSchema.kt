package com.github.inkm3.yamlconfig.schema.collection

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.schema.internal.YamlSchemaSupport

internal class YamlListSchema<T>(
    internal val elementSchema: YamlSchema<T>,
): YamlSchema<List<T>>() {

    override fun decode(node: YamlNode, path: YamlPath): List<T> {
        if (node !is YamlSequenceNode) {
            throw YamlSchemaException(path, "Expected sequence, but found ${YamlSchemaSupport.nodeType(node)}")
        }

        return node.elements.mapIndexed { index, element ->
            elementSchema.decode(element, path.child(index))
        }
    }

    override fun encode(value: List<T>): YamlNode {
        return YamlSequenceNode(value.map(elementSchema::encode))
    }

    override fun equivalent(first: List<T>, second: List<T>): Boolean {
        if (first.size != second.size) {
            return false
        }

        return first.indices.all { index ->
            elementSchema.equivalent(first[index], second[index])
        }
    }
}