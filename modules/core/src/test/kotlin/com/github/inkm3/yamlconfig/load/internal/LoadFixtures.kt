package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
internal data class LoadDatabase(val host: String = "localhost", val port: Int = 3306)

@Serializable
internal data class LoadConfig(
    @SerialName("server-name") val name: String = "kotlin-name",
    val database: LoadDatabase = LoadDatabase(host = "prod"),
    val servers: List<LoadDatabase> = emptyList(),
    val ports: Map<String, Int> = emptyMap(),
    val message: String? = "kotlin-message",
)

internal object LoadFixtures {
    fun string(value: String): YamlScalarNode = YamlScalarNode(value, YamlScalarKind.STRING)
    fun integer(value: Int): YamlScalarNode = YamlScalarNode(value.toString(), YamlScalarKind.INTEGER)
    fun nullNode(): YamlScalarNode = YamlScalarNode("null", YamlScalarKind.NULL)
    fun mapping(vararg entries: Pair<String, YamlNode>): YamlMappingNode =
        YamlMappingNode(entries.associateTo(linkedMapOf()) { YamlMapKey(it.first) to it.second })
    fun sequence(vararg elements: YamlNode): YamlSequenceNode = YamlSequenceNode(elements.toList())

    inline fun <reified T> load(
        defaults: YamlNode? = null,
        user: YamlNode? = null,
        format: YamlSerialization = YamlSerialization.Default,
    ): T = YamlConfigValueLoader(format.serializersModule.serializer<T>(), format).load(defaults, user)
}
