package com.github.inkm3.yamlconfig.node

import java.util.*

public class YamlSequenceNode(
    elements: Collection<YamlNode>,
): YamlNode {

    public val elements: List<YamlNode> = Collections.unmodifiableList(ArrayList(elements))

    public val size: Int get() = elements.size

    public operator fun get(index: Int): YamlNode {
        return elements[index]
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is YamlSequenceNode && elements == other.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }

    override fun toString(): String {
        return "YamlSequenceNode(elements=$elements)"
    }
}