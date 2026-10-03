package com.github.inkm3.yamlconfig.save.internal.serialized

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.reduction.YamlRemovalCandidate
import com.github.inkm3.yamlconfig.save.internal.serialized.reduction.YamlRemovalCandidates
import kotlinx.serialization.descriptors.SerialDescriptor

internal data class YamlOverrideReduction(val root: YamlNode?, val removals: List<YamlValuePatch>)

/** Greedy safe reductions against the evolving FULL document, not stale defaults. */
internal object YamlMinimalOverrideReducer {
    /** Compatibility for callers that need only replayable patches. */
    internal fun reduce(descriptor: SerialDescriptor, initial: YamlNode?,
        matches: (YamlNode?) -> Boolean): List<YamlValuePatch> = reduceToResult(descriptor, initial, matches).removals

    internal fun reduceToResult(descriptor: SerialDescriptor, initial: YamlNode?,
        matches: (YamlNode?) -> Boolean): YamlOverrideReduction {
        if (initial == null) return YamlOverrideReduction(null, emptyList())
        if (initial is YamlScalarNode) {
            // A scalar has no unknown descendants or child removal paths. Keep
            // the same single full-document check without building a path list.
            return if (matches(null)) YamlOverrideReduction(null, listOf(YamlValuePatch(emptyList(), null)))
                else YamlOverrideReduction(initial, emptyList())
        }
        var root: YamlNode? = initial
        val removals = mutableListOf<YamlValuePatch>()
        val pending = YamlRemovalCandidates.collect(descriptor, initial).toMutableList()
        do {
            var changed = false
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val keys = iterator.next()
                val candidate = YamlRemovalCandidate.remove(root, keys)
                if (candidate === root) {
                    // A previous parent deletion made this path permanently absent.
                    iterator.remove()
                } else if (matches(candidate)) {
                    root = candidate
                    // Build a replayable editor patch only for an accepted deletion.
                    removals += YamlValuePatch(keys, null)
                    iterator.remove()
                    changed = true
                }
            }
            // Rejected paths must be retried after another deletion: defaults can
            // depend on siblings. Removing paths cannot create new eligible nodes.
        } while (changed)
        return YamlOverrideReduction(root, removals)
    }
}
