package com.github.inkm3.yamlconfig.save.internal.diff

internal object YamlSequenceDiff {

    internal fun <T> calculate(
        baseline: List<T>,
        current: List<T>,
        equivalent: (T, T) -> Boolean,
    ): List<YamlSequenceEdit<T>> {
        val working = baseline.toMutableList()
        val edits = mutableListOf<YamlSequenceEdit<T>>()
        for (targetIndex in current.indices) {
            val targetValue = current[targetIndex]
            if (
                targetIndex < working.size &&
                equivalent(working[targetIndex], targetValue)
            ) {
                continue
            }

            val matchingIndex = findEquivalentAfter(
                working,
                targetValue,
                targetIndex + 1,
                equivalent
            )

            if (matchingIndex >= 0) {
                val moved = working.removeAt(matchingIndex)
                working.add(targetIndex, moved)

                edits += YamlSequenceEdit.Move<T>(
                    matchingIndex,
                    targetIndex,
                )

                continue
            }

            val existingNeededLater = if (targetIndex < working.size) {
                isNeededLater(
                    working[targetIndex],
                    current,
                    targetIndex + 1,
                    equivalent,
                )
            } else {
                false
            }

            if (
                targetIndex >= working.size ||
                existingNeededLater
            ) {
                working.add(targetIndex, targetValue)

                edits += YamlSequenceEdit.Insert(
                    targetIndex,
                    targetValue,
                )

                continue
            }

            val previous = working[targetIndex]
            working[targetIndex] = targetValue

            edits += YamlSequenceEdit.Update(
                targetIndex,
                previous,
                targetValue,
            )
        }

        for (index in working.lastIndex downTo current.size) {
            working.removeAt(index)


            edits += YamlSequenceEdit.Remove<T>(index)
        }

        return edits
    }

    private fun <T> findEquivalentAfter(
        values: List<T>,
        value: T,
        startIndex: Int,
        equivalent: (T, T) -> Boolean,
    ): Int {
        for (index in startIndex until values.size) {
            if (equivalent(values[index], value)) {
                return index
            }
        }

        return -1
    }

    private fun <T> isNeededLater(
        value: T,
        current: List<T>,
        startIndex: Int,
        equivalent: (T, T) -> Boolean,
    ): Boolean {
        for (index in startIndex until current.size) {
            if (equivalent(value, current[index])) {
                return true
            }
        }

        return false
    }
}