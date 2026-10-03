package com.github.inkm3.yamlconfig.save.internal.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode

/** Relative tree edits. A null Set removes a mapping property or the root. */
internal sealed interface YamlStructuralEdit {
    data class Set(val value: YamlNode?) : YamlStructuralEdit
    data class Mapping(val changes: Map<YamlMapKey, YamlStructuralEdit>) : YamlStructuralEdit
    data class Sequence(val steps: List<YamlSequenceStep>) : YamlStructuralEdit
}

/** Indices address the sequence AFTER all preceding steps, not the initial list. */
internal sealed interface YamlSequenceStep {
    data class Insert(val index: Int, val value: YamlNode) : YamlSequenceStep
    data class Remove(val index: Int) : YamlSequenceStep
    data class Move(val from: Int, val to: Int) : YamlSequenceStep
    data class Update(val index: Int, val edit: YamlStructuralEdit) : YamlSequenceStep
}
