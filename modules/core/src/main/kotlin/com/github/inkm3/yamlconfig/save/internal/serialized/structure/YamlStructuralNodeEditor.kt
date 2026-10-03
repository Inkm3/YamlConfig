package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode

/** Pure candidate construction. Never mutates input nodes or their collections. */
internal object YamlStructuralNodeEditor {
    internal fun apply(root: YamlNode?, edit: YamlStructuralEdit): YamlNode? = when (edit) {
        is YamlStructuralEdit.Set -> edit.value
        is YamlStructuralEdit.Mapping -> mapping(root, edit)
        is YamlStructuralEdit.Sequence -> sequence(root, edit)
    }

    private fun mapping(root: YamlNode?, edit: YamlStructuralEdit.Mapping): YamlNode? {
        require(root == null || root is YamlMappingNode) { "Expected mapping for structural edit" }
        val entries = LinkedHashMap<YamlMapKey, YamlNode>((root as? YamlMappingNode)?.entries ?: emptyMap())
        for ((key, change) in edit.changes) {
            val value = apply(entries[key], change)
            if (value == null) entries.remove(key) else entries[key] = value
        }
        return if (root == null && entries.isEmpty()) null else YamlMappingNode(entries)
    }

    private fun sequence(root: YamlNode?, edit: YamlStructuralEdit.Sequence): YamlNode {
        require(root is YamlSequenceNode) { "Expected existing sequence for structural edit" }
        val elements = root.elements.toMutableList()
        for (step in edit.steps) {
            when (step) {
                is YamlSequenceStep.Insert -> {
                    require(step.index in 0..elements.size)
                    elements.add(step.index, step.value)
                }
                is YamlSequenceStep.Remove -> {
                    require(step.index in elements.indices)
                    elements.removeAt(step.index)
                }
                is YamlSequenceStep.Move -> {
                    require(step.from in elements.indices && step.to in elements.indices)
                    elements.add(step.to, elements.removeAt(step.from))
                }
                is YamlSequenceStep.Update -> {
                    require(step.index in elements.indices)
                    elements[step.index] = requireNotNull(apply(elements[step.index], step.edit)) {
                        "Use Remove, not a null Update, to delete a sequence element"
                    }
                }
            }
        }
        return YamlSequenceNode(elements)
    }
}
