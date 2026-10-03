package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.internal.atYamlPath
import com.github.inkm3.yamlconfig.serialization.internal.decoder.YamlNodeRootDecoder
import com.github.inkm3.yamlconfig.serialization.internal.encoder.YamlNodeRootEncoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

/**
 * Converts Kotlin values to/from semantic YAML nodes; it does not parse YAML text.
 *
 * Serializer annotations (including EncodeDefault) take precedence over
 * [encodeDefaults]. An encoded node is therefore NOT a guaranteed complete
 * snapshot of every Kotlin property and must not be treated as a save diff.
 *
 * Instances are reusable: mutable encoder/decoder state belongs to each call.
 */
public class YamlSerialization @JvmOverloads constructor(
    override val serializersModule: SerializersModule = EmptySerializersModule(),
    public val encodeDefaults: Boolean = true,
    public val ignoreUnknownKeys: Boolean = false,
) : SerialFormat {

    public fun <T> encodeToNode(
        serializer: SerializationStrategy<T>,
        value: T,
    ): YamlNode = atYamlPath(YamlPath.root()) {
        val encoder = YamlNodeRootEncoder(this)
        encoder.encodeSerializableValue(serializer, value)
        encoder.result()
    }

    public inline fun <reified T> encodeToNode(value: T): YamlNode =
        encodeToNode(serializersModule.serializer<T>(), value)

    public fun <T> decodeFromNode(
        deserializer: DeserializationStrategy<T>,
        node: YamlNode,
    ): T = YamlNodeRootDecoder(this, node).decodeSerializableValue(deserializer)

    public inline fun <reified T> decodeFromNode(node: YamlNode): T =
        decodeFromNode(serializersModule.serializer<T>(), node)

    public companion object {
        @JvmStatic
        public val Default: YamlSerialization = YamlSerialization()
    }
}
