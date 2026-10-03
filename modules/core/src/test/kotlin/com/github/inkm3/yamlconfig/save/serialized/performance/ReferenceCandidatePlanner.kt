package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlKnownValuePlanner
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlMinimalOverrideReducer
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidate
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlValuePatch
import com.github.inkm3.yamlconfig.save.internal.serialized.omission.YamlOmittedPropertyPruner
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor

/** Frozen control: YamlSaveCandidatePlanner at bc407116 (before memoization).
 * Keep this independent of future candidate-evaluation optimizations. The shared
 * patch builder/reducer/pruner are deliberately NOT the subject of this comparison.
 */
internal class ReferenceCandidatePlanner(
    private val descriptor: SerialDescriptor,
    private val encodeReload: (YamlNode?) -> YamlNode,
) {
    fun plan(baseline: YamlNode, expected: YamlNode, user: YamlNode?, mode: YamlSaveMode): YamlSaveCandidate {
        var candidate = select(baseline, expected, user)
        if (mode == YamlSaveMode.MINIMAL_DIFFERENCE) {
            val removals = YamlMinimalOverrideReducer.reduce(descriptor, candidate.root) { matches(it, expected) }
            candidate = YamlSaveCandidate(apply(candidate.root, removals), candidate.patches + removals)
        }
        return candidate
    }

    private fun select(baseline: YamlNode, expected: YamlNode, user: YamlNode?): YamlSaveCandidate {
        for (force in listOf(false, true)) {
            val retained = create(baseline, expected, user, force, retainOmitted = true)
            repairOmissions(retained, expected)?.let { return it }
        }
        val ordinary = create(baseline, expected, user, force = false, retainOmitted = false)
        if (matches(ordinary.root, expected)) return ordinary
        val forced = create(baseline, expected, user, force = true, retainOmitted = false)
        requireMatches(forced.root, expected)
        return forced
    }

    private fun repairOmissions(initial: YamlSaveCandidate, expected: YamlNode): YamlSaveCandidate? {
        var candidate = initial
        while (true) {
            val actual = try { encodeReload(candidate.root) } catch (_: SerializationException) { return null }
            if (actual == expected) return candidate
            val edit = YamlOmittedPropertyPruner.plan(descriptor, expected, actual, candidate.root) ?: return null
            val patch = YamlValuePatch.structural(edit)
            val root = patch.applyTo(candidate.root)
            if (root == candidate.root) return null
            candidate = YamlSaveCandidate(root, candidate.patches + patch)
        }
    }

    private fun create(baseline: YamlNode, expected: YamlNode, user: YamlNode?, force: Boolean,
        retainOmitted: Boolean): YamlSaveCandidate {
        val patches = YamlKnownValuePlanner.plan(descriptor, baseline, expected, user, force, retainOmitted)
        return YamlSaveCandidate(apply(user, patches), patches)
    }

    private fun apply(root: YamlNode?, patches: List<YamlValuePatch>): YamlNode? =
        patches.fold(root) { node, patch -> patch.applyTo(node) }

    private fun matches(root: YamlNode?, expected: YamlNode): Boolean = try {
        encodeReload(root) == expected
    } catch (_: SerializationException) { false }

    private fun requireMatches(root: YamlNode?, expected: YamlNode) {
        if (encodeReload(root) != expected) throw SerializationException(
            "Cannot save without changing the serialized value; an omitted property or custom serializer may not " +
                "expose the required override. Remove EncodeDefault(NEVER), use standard EncodeDefault(ALWAYS), " +
                "or provide a serializer that writes the needed value. Output was not written.",
        )
    }
}
