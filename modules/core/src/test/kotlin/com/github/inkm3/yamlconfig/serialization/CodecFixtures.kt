package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import kotlinx.serialization.Serializable
import kotlin.test.assertEquals

@Serializable
internal data class CodecServer(val name: String, val port: Int = 25565)

@Serializable
internal data class CodecConfig(val servers: List<CodecServer> = emptyList())

internal object CodecFixtures {
    fun string(value: String): YamlScalarNode = YamlScalarNode(value, YamlScalarKind.STRING)
    fun integer(value: String): YamlScalarNode = YamlScalarNode(value, YamlScalarKind.INTEGER)
    fun integer(value: Int): YamlScalarNode = integer(value.toString())
    fun floating(value: String): YamlScalarNode = YamlScalarNode(value, YamlScalarKind.FLOAT)
    fun nullNode(): YamlScalarNode = YamlScalarNode("null", YamlScalarKind.NULL)
    fun sequence(vararg nodes: YamlNode): YamlSequenceNode = YamlSequenceNode(nodes.toList())
    fun mapping(vararg fields: Pair<String, YamlNode>): YamlMappingNode = YamlMappingNode(
        linkedMapOf(*fields.map { (name, node) -> YamlMapKey(name) to node }.toTypedArray()),
    )

    inline fun <reified T> roundTrip(value: T, expected: YamlNode) {
        val format = YamlSerialization.Default
        assertEquals(expected, format.encodeToNode(value))
        assertEquals(value, format.decodeFromNode<T>(expected))
    }
}
