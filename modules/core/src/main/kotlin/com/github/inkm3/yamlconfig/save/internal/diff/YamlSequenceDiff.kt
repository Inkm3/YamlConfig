package com.github.inkm3.yamlconfig.save.internal.diff

internal object YamlSequenceDiff {
    /** Preserve existing call sites, including trailing equivalent lambdas. */
    internal fun <T> calculate(
        baseline: List<T>, current: List<T>, equivalent: (T, T) -> Boolean,
    ): List<YamlSequenceEdit<T>> = calculate(baseline, current, equivalent, { _, _ -> false }, { _, _ -> true })

    /** Identity/permission callbacks must be stable throughout the calculation. */
    internal fun <T> calculate(
        baseline: List<T>,
        current: List<T>,
        equivalent: (T, T) -> Boolean,
        sameItem: (T, T) -> Boolean,
        canUpdate: (T, T) -> Boolean,
    ): List<YamlSequenceEdit<T>> {
        val working = baseline.toMutableList()
        val edits = mutableListOf<YamlSequenceEdit<T>>()
        for (targetIndex in current.indices) {
            val target = current[targetIndex]
            if (targetIndex < working.size && equivalent(working[targetIndex], target)) continue

            val exact = findAfter(working, target, targetIndex + 1, equivalent)
            if (exact >= 0) {
                working.add(targetIndex, working.removeAt(exact))
                edits += YamlSequenceEdit.Move<T>(exact, targetIndex)
                continue
            }

            val related = findAfter(working, target, targetIndex, sameItem)
            if (related >= 0) {
                if (related != targetIndex) {
                    working.add(targetIndex, working.removeAt(related))
                    edits += YamlSequenceEdit.Move<T>(related, targetIndex)
                }
                val previous = working[targetIndex]
                working[targetIndex] = target
                edits += YamlSequenceEdit.Update(targetIndex, previous, target)
                continue
            }

            val neededLater = targetIndex < working.size && (targetIndex + 1 until current.size).any {
                equivalent(working[targetIndex], current[it]) || sameItem(working[targetIndex], current[it])
            }
            if (targetIndex >= working.size || neededLater) {
                working.add(targetIndex, target)
                edits += YamlSequenceEdit.Insert(targetIndex, target)
            } else if (canUpdate(working[targetIndex], target)) {
                val previous = working[targetIndex]
                working[targetIndex] = target
                edits += YamlSequenceEdit.Update(targetIndex, previous, target)
            } else {
                // Different/ambiguous identities: do not pass the old node to an
                // object Update, which could copy someone else's unknown fields.
                working.removeAt(targetIndex)
                edits += YamlSequenceEdit.Remove<T>(targetIndex)
                working.add(targetIndex, target)
                edits += YamlSequenceEdit.Insert(targetIndex, target)
            }
        }
        for (index in working.lastIndex downTo current.size) {
            working.removeAt(index)
            edits += YamlSequenceEdit.Remove<T>(index)
        }
        return edits
    }

    private fun <T> findAfter(values: List<T>, value: T, start: Int, matches: (T, T) -> Boolean): Int {
        for (index in start until values.size) if (matches(values[index], value)) return index
        return -1
    }
}
