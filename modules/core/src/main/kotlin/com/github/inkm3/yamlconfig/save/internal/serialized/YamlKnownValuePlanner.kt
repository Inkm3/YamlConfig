package com.github.inkm3.yamlconfig.save.internal.serialized

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignmentMemo
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralValuePlanner
import kotlinx.serialization.descriptors.SerialDescriptor

/** The same planned edits construct the candidate and later edit the native tree. */
internal object YamlKnownValuePlanner {
    internal fun plan(
        descriptor: SerialDescriptor,
        baseline: YamlNode?,
        current: YamlNode?,
        user: YamlNode?,
        force: Boolean = false,
        retainOmitted: Boolean = false,
        alignment: YamlSourceAlignmentMemo = YamlSourceAlignmentMemo(),
    ): List<YamlValuePatch> {
        val edit = YamlStructuralValuePlanner.plan(descriptor, baseline, current, user, force,
            root = true, retainOmitted = retainOmitted, alignment = alignment) ?: return emptyList()
        return listOf(YamlValuePatch.structural(edit))
    }

    internal fun isObject(descriptor: SerialDescriptor): Boolean = YamlStructuralValuePlanner.isObject(descriptor)
}
