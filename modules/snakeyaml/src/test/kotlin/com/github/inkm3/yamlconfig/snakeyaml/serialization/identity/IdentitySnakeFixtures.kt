package com.github.inkm3.yamlconfig.snakeyaml.serialization.identity

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.YamlInput
import kotlinx.serialization.Serializable
import kotlin.test.assertIs

@Serializable
internal data class IdentityServer(val name: String, val port: Int = 25565)

internal fun identityItems(input: YamlInput): List<YamlMappingNode> =
    assertIs<YamlSequenceNode>(SnakeYamlEngine().parse(input).root).elements.map { assertIs<YamlMappingNode>(it) }

internal fun YamlMappingNode.text(key: String): String? = (this[key] as? YamlScalarNode)?.value
