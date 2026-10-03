package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.serialization.internal.failYaml
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.StructureKind

/** Read-only Config decoding: no I/O, encoding, baseline creation or save policy. */
@OptIn(ExperimentalSerializationApi::class)
internal class YamlConfigValueLoader<T>(
    private val deserializer: DeserializationStrategy<T>,
    serialization: YamlSerialization,
) {
    // Config loading tolerates unknown string properties, without changing the
    // caller's format or losing contextual registrations. Known values stay strict.
    private val decoding = if (serialization.ignoreUnknownKeys) serialization else YamlSerialization(
        serializersModule = serialization.serializersModule,
        encodeDefaults = serialization.encodeDefaults,
        ignoreUnknownKeys = true,
    )

    internal fun load(defaultsRoot: YamlNode?, userRoot: YamlNode?): T {
        val descriptor = deserializer.descriptor
        val effective = YamlNodeOverlay.overlay(descriptor, defaultsRoot, userRoot)
            ?: if (!descriptor.isNullable && !descriptor.isInline &&
                (descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT)
            ) {
                // Root-only bootstrap. Never materialize an absent nested field.
                YamlMappingNode(emptyMap())
            } else {
                failYaml(YamlPath.root(), "Missing YAML root for ${descriptor.serialName}")
            }
        // Decode once, after selection. A shadowed default need not decode alone.
        return decoding.decodeFromNode(deserializer, effective)
    }
}
