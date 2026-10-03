package com.github.inkm3.yamlconfig.save.internal.serialized.validation

import com.github.inkm3.yamlconfig.node.YamlNode
import kotlinx.serialization.SerializationException

/**
 * One plan's bounded memo for a deterministic decode/encode callback. Do not keep
 * this on a session/planner, share it between saves, or use it for final Editor
 * validation. Retains at most capacity input/result pairs; no unbounded tree keys.
 * Exact order-sensitive comparison avoids allocation of a full-document key/hash.
 */
internal class YamlCandidateValidation(
    private val encodeReload: (YamlNode?) -> YamlNode,
    private val capacity: Int = 16,
) {
    private class Entry(val root: YamlNode?, val outcome: Result<YamlNode>)
    private var latest: Entry? = null
    private var older: ArrayList<Entry>? = null

    init { require(capacity > 0) }

    internal fun evaluate(root: YamlNode?): YamlNode {
        val previous = latest
        if (previous != null && YamlOrderedNodeEquality.same(previous.root, root)) {
            return previous.outcome.getOrThrow()
        }
        older?.let { entries ->
            for (index in entries.lastIndex downTo 0) {
                val entry = entries[index]
                if (YamlOrderedNodeEquality.same(entry.root, root)) return entry.outcome.getOrThrow()
            }
        }
        val outcome = try {
            Result.success(encodeReload(root))
        } catch (failure: SerializationException) {
            // Failed decoding is a result for this exact document too. Preserve
            // its original type/cause. Unexpected failures are NOT cached/caught.
            Result.failure(failure)
        }
        if (previous != null && capacity > 1) {
            val entries = older ?: ArrayList<Entry>(capacity - 1).also { older = it }
            if (entries.size == capacity - 1) entries.removeAt(0)
            entries.add(previous)
        }
        latest = Entry(root, outcome)
        return outcome.getOrThrow()
    }
}
