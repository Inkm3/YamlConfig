package com.github.inkm3.yamlconfig.save.internal.diff

/**
 * Exact-first greedy diff, equivalent to YamlSequenceDiff for stable values.
 * Equal values MUST have equal hashes; collisions are always checked by equality.
 * identity must return a stable unique-in-both-lists key, or null for ambiguity.
 * Array movement/position maintenance is still quadratic in the worst case.
 */
internal object YamlIndexedSequenceDiff {
    internal fun <T> calculate(
        baseline: List<T>, current: List<T>, equivalent: (T, T) -> Boolean,
        hash: (T) -> Int, identity: (T) -> Any?, canUpdate: (T, T) -> Boolean,
    ): List<YamlSequenceEdit<T>> {
        val working = YamlSequenceIndex(baseline, hash, identity)
        val targetHashes = current.map(hash)
        val targetKeys = current.map(identity)
        val futureExact = HashMap<Int, MutableList<Int>>()
        val futureIdentity = HashMap<Any, MutableList<Int>>()
        for (index in current.indices) {
            futureExact.getOrPut(targetHashes[index]) { ArrayList() }.add(index)
            targetKeys[index]?.let { futureIdentity.getOrPut(it) { ArrayList() }.add(index) }
        }
        val edits = ArrayList<YamlSequenceEdit<T>>()
        for (index in current.indices) {
            val target = current[index]
            if (index < working.size && equivalent(working[index].value, target)) continue
            val exact = working.findExact(index + 1, target, targetHashes[index], equivalent)
            if (exact >= 0) {
                working.move(exact, index)
                edits += YamlSequenceEdit.Move<T>(exact, index)
                continue
            }
            val related = working.findIdentity(index, targetKeys[index])
            if (related >= 0) {
                if (related != index) {
                    working.move(related, index)
                    edits += YamlSequenceEdit.Move<T>(related, index)
                }
                val previous = working.replace(index, target)
                edits += YamlSequenceEdit.Update(index, previous, target)
                continue
            }
            val neededLater = if (index >= working.size) false else {
                val entry = working[index]
                futureExact[entry.hash].orEmpty().any { it > index && equivalent(entry.value, current[it]) } ||
                    (entry.identity != null && futureIdentity[entry.identity].orEmpty().any { it > index })
            }
            if (index >= working.size || neededLater) {
                working.insert(index, target)
                edits += YamlSequenceEdit.Insert(index, target)
            } else if (canUpdate(working[index].value, target)) {
                val previous = working.replace(index, target)
                edits += YamlSequenceEdit.Update(index, previous, target)
            } else {
                working.remove(index)
                edits += YamlSequenceEdit.Remove<T>(index)
                working.insert(index, target)
                edits += YamlSequenceEdit.Insert(index, target)
            }
        }
        for (index in working.size - 1 downTo current.size) {
            working.remove(index)
            edits += YamlSequenceEdit.Remove<T>(index)
        }
        return edits
    }
}
