package com.github.inkm3.yamlconfig.save.internal.serialized

import com.github.inkm3.yamlconfig.load.internal.YamlConfigValueLoader
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.session.YamlPreparedSave
import com.github.inkm3.yamlconfig.save.internal.session.YamlSaveTransaction
import com.github.inkm3.yamlconfig.save.internal.session.YamlSessionSaver
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import kotlinx.serialization.KSerializer

/**
 * Equality means equality of this serializer's normalized node representation.
 * Transient/unserialized state is outside this contract. Serializers must be
 * deterministic; annotations are respected, not assumed to expose all properties.
 */
internal class YamlSerializedSessionSaver<T>(
    initial: T,
    private val serializer: KSerializer<T>,
    serialization: YamlSerialization,
    private val defaults: YamlNode?,
    editor: YamlEditor,
    engine: YamlEngine,
    output: YamlOutput,
    private val mode: YamlSaveMode,
) : YamlSessionSaver<T> {
    private val format = YamlSerialization(serialization.serializersModule, encodeDefaults = true, ignoreUnknownKeys = true)
    private val loader = YamlConfigValueLoader(serializer, format)
    private val planner = YamlSaveCandidatePlanner(serializer.descriptor) { encode(loader.load(defaults, it)) }
    private val transaction = YamlSaveTransaction(encode(initial), editor, engine, output)

    override fun save(current: T) {
        transaction.save prepare@ { baseline, user ->
            val expected = encode(current)
            if (mode == YamlSaveMode.PRESERVE_OVERRIDES && baseline == expected) return@prepare null
            val candidate = planner.plan(baseline, expected, user, mode)
            if (candidate.root == user) return@prepare null
            YamlPreparedSave(expected) { working ->
                candidate.patches.forEach { it.apply(working) }
                // Validate actual editor behavior too, before output is touched.
                planner.requireMatches(working.root, expected)
            }
        }
    }

    private fun encode(value: T): YamlNode = format.encodeToNode(serializer, value)
}
