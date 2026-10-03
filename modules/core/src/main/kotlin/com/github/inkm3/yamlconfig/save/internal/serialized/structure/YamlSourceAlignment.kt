package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/** Representation alignment, not identity proof for transforming serializers.
 * Optional encoded omissions can be tolerated only by the validated save path.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object YamlSourceAlignment {
    internal fun matches(
        descriptor: SerialDescriptor, baseline: YamlNode?, user: YamlNode?, allowOmitted: Boolean = false,
    ): Boolean {
        if (baseline == user) return true
        if (baseline == null || user == null || descriptor.isInline) return false
        if (baseline is YamlScalarNode && user is YamlScalarNode) {
            val old = YamlScalarRepresentation.normalize(descriptor, YamlMapKey(baseline.value, baseline.kind))
            val actual = YamlScalarRepresentation.normalize(descriptor, YamlMapKey(user.value, user.kind))
            return old != null && old == actual
        }
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> {
                if (baseline !is YamlMappingNode || user !is YamlMappingNode) return false
                user.entries.all { (key, value) ->
                    if (key.kind != YamlScalarKind.STRING) return@all false
                    val index = descriptor.getElementIndex(key.value)
                    if (index !in 0 until descriptor.elementsCount) true
                    else if (allowOmitted && baseline[key] == null && descriptor.isElementOptional(index)) true
                    else matches(descriptor.getElementDescriptor(index), baseline[key], value, allowOmitted)
                }
            }
            StructureKind.LIST -> {
                if (baseline !is YamlSequenceNode || user !is YamlSequenceNode || baseline.size != user.size) return false
                val element = descriptor.getElementDescriptor(0)
                baseline.elements.indices.all { matches(element, baseline[it], user[it], allowOmitted) }
            }
            StructureKind.MAP -> {
                if (baseline !is YamlMappingNode || user !is YamlMappingNode) return false
                val keyDescriptor = descriptor.getElementDescriptor(0)
                val before = YamlScalarRepresentation.index(keyDescriptor, baseline.entries.keys) ?: return false
                val source = YamlScalarRepresentation.index(keyDescriptor, user.entries.keys) ?: return false
                before.keys == source.keys && before.all { (key, raw) ->
                    matches(descriptor.getElementDescriptor(1), baseline[raw], user[source.getValue(key)], allowOmitted)
                }
            }
            else -> false
        }
    }
}
