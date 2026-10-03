package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Selects user values before defaults. Only non-inline object descriptors merge
 * known properties; collections, contextual and other representations are atomic.
 * Kotlin null means missing, never an explicit YAML null.
 *
 * Neither input is modified. Missing children remain missing so the deserializer,
 * not this overlay, decides constructor defaults and required-field failures.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object YamlNodeOverlay {
    internal fun overlay(
        descriptor: SerialDescriptor,
        defaults: YamlNode?,
        user: YamlNode?,
    ): YamlNode? {
        if (user == null) return defaults
        if (defaults == null || user === defaults) return user
        if (descriptor.isInline ||
            (descriptor.kind != StructureKind.CLASS && descriptor.kind != StructureKind.OBJECT) ||
            defaults !is YamlMappingNode || user !is YamlMappingNode
        ) return user

        // Copy only when a known property actually inherits something. Retain
        // user entries so merging cannot hide invalid non-string object keys.
        var merged: LinkedHashMap<YamlMapKey, YamlNode>? = null
        for (index in 0 until descriptor.elementsCount) {
            val key = YamlMapKey(descriptor.getElementName(index))
            val defaultValue = defaults[key] ?: continue
            val userValue = user[key]
            val value = if (userValue == null) {
                defaultValue
            } else {
                overlay(descriptor.getElementDescriptor(index), defaultValue, userValue)
            }
            if (value != null && value !== userValue) {
                val entries = merged ?: LinkedHashMap(user.entries).also { merged = it }
                entries[key] = value
            }
        }
        return merged?.let(::YamlMappingNode) ?: user
    }
}
