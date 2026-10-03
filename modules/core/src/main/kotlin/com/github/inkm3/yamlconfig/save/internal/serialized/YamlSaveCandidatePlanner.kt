package com.github.inkm3.yamlconfig.save.internal.serialized

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.omission.YamlOmittedPropertyPruner
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignmentMemo
import com.github.inkm3.yamlconfig.save.internal.serialized.validation.YamlCandidateValidation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor

internal data class YamlSaveCandidate(val root: YamlNode?, val patches: List<YamlValuePatch>)

/** Pure planning/validation boundary, independent of session state, editors and I/O. */
internal class YamlSaveCandidatePlanner(
    private val descriptor: SerialDescriptor,
    private val encodeReload: (YamlNode?) -> YamlNode,
) {
    internal fun plan(
        baseline: YamlNode, expected: YamlNode, user: YamlNode?, mode: YamlSaveMode,
    ): YamlSaveCandidate {
        // Both memos die with this invocation; neither validates the real Editor.
        val validation = YamlCandidateValidation(encodeReload)
        val alignment = YamlSourceAlignmentMemo()
        var candidate = select(baseline, expected, user, validation, alignment)
        if (mode == YamlSaveMode.MINIMAL_DIFFERENCE) {
            val reduction = YamlMinimalOverrideReducer.reduceToResult(descriptor, candidate.root) {
                matches(it, expected, validation)
            }
            candidate = YamlSaveCandidate(reduction.root, candidate.patches + reduction.removals)
        }
        return candidate
    }

    private fun select(baseline: YamlNode, expected: YamlNode, user: YamlNode?,
        validation: YamlCandidateValidation, alignment: YamlSourceAlignmentMemo): YamlSaveCandidate {
        for (force in listOf(false, true)) {
            val retained = create(baseline, expected, user, force, retainOmitted = true, alignment)
            repairOmissions(retained, expected, validation)?.let { return it }
        }
        val ordinary = create(baseline, expected, user, force = false, retainOmitted = false, alignment)
        if (matches(ordinary.root, expected, validation)) return ordinary
        val forced = create(baseline, expected, user, force = true, retainOmitted = false, alignment)
        requireEqual(validation.evaluate(forced.root), expected)
        return forced
    }

    private fun repairOmissions(initial: YamlSaveCandidate, expected: YamlNode,
        validation: YamlCandidateValidation): YamlSaveCandidate? {
        var candidate = initial
        while (true) {
            val actual = try { validation.evaluate(candidate.root) } catch (_: SerializationException) { return null }
            if (actual == expected) return candidate
            val edit = YamlOmittedPropertyPruner.plan(descriptor, expected, actual, candidate.root) ?: return null
            val patch = YamlValuePatch.structural(edit)
            val root = patch.applyTo(candidate.root)
            if (root == candidate.root) return null
            candidate = YamlSaveCandidate(root, candidate.patches + patch)
        }
    }

    private fun create(
        baseline: YamlNode, expected: YamlNode, user: YamlNode?, force: Boolean,
        retainOmitted: Boolean, alignment: YamlSourceAlignmentMemo,
    ): YamlSaveCandidate {
        val patches = YamlKnownValuePlanner.plan(descriptor, baseline, expected, user, force, retainOmitted, alignment)
        return YamlSaveCandidate(apply(user, patches), patches)
    }

    private fun apply(root: YamlNode?, patches: List<YamlValuePatch>): YamlNode? =
        patches.fold(root) { node, patch -> patch.applyTo(node) }

    private fun matches(root: YamlNode?, expected: YamlNode, validation: YamlCandidateValidation): Boolean = try {
        validation.evaluate(root) == expected
    } catch (_: SerializationException) { false }

    /** Final actual-Editor validation is always a fresh callback, NOT memoized. */
    internal fun requireMatches(root: YamlNode?, expected: YamlNode) {
        requireEqual(encodeReload(root), expected)
    }

    private fun requireEqual(actual: YamlNode, expected: YamlNode) {
        if (actual != expected) throw SerializationException(
            "Cannot save without changing the serialized value; an omitted property or custom serializer may not " +
                "expose the required override. Remove EncodeDefault(NEVER), use standard EncodeDefault(ALWAYS), " +
                "or provide a serializer that writes the needed value. Output was not written.",
        )
    }
}
